package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmailService;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingCancellation;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingCancellationRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.impl.BookingSocketEmitter;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.PaymentTransaction;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentStatus;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.OrderRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.CashPaymentRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.PaymentRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.PaymentResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.PaymentTransactionResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.WebhookResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final PayOS payOS;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingCancellationRepository cancellationRepository;
    private final EmailService emailService;
    private final BookingSocketEmitter bookingSocketEmitter;
    private final String frontendUrl;


    // Tự viết Constructor tường minh để khởi tạo đầy đủ các bean và thông tin cấu hình PayOS
    public PaymentServiceImpl(
            OrderRepository orderRepository,
            ObjectMapper objectMapper,
            @Value("${payos.client-id}") String clientId,
            @Value("${payos.api-key}") String apiKey,
            @Value("${payos.checksum-key}") String checksumKey,
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            BookingCancellationRepository cancellationRepository,
            EmailService emailService,
            BookingSocketEmitter bookingSocketEmitter,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.paymentRepository = paymentRepository;
        this.payOS = new PayOS(clientId, apiKey, checksumKey);
        this.bookingRepository = bookingRepository;
        this.cancellationRepository = cancellationRepository;
        this.emailService = emailService;
        this.bookingSocketEmitter = bookingSocketEmitter;
        this.frontendUrl = frontendUrl;
    }

    private void reconcileLatePayment(Order order, long amountPaid) {
        Booking booking = order.getBooking();
        if (booking == null || !booking.getBookingStatus().isCancelled()) {
            return;
        }

        BookingCancellation cancellation = cancellationRepository.findByBookingIdForUpdate(booking.getId())
                .orElse(null);
        if (cancellation == null) {
            log.error("Received PayOS payment after booking cancellation without a cancellation record, bookingId={}",
                    booking.getId());
            return;
        }

        BigDecimal updatedRefundAmount = (cancellation.getRefundAmount() == null
                ? BigDecimal.ZERO
                : cancellation.getRefundAmount()).add(BigDecimal.valueOf(amountPaid));
        cancellation.setRefundAmount(updatedRefundAmount);
        cancellation.setReviewNote("PayOS payment of " + amountPaid
                + " received after booking cancellation; refund requires review.");

        if (cancellation.getStatus() == BookingCancellationStatus.NO_REFUND_REQUIRED
                || cancellation.getStatus() == BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED
                || cancellation.getStatus() == BookingCancellationStatus.REFUND_REVIEW_PENDING) {
            boolean hasBankDetails = cancellation.getBankName() != null
                    && cancellation.getBankCode() != null
                    && cancellation.getBankAccountName() != null
                    && cancellation.getBankAccountNumber() != null;
            cancellation.setStatus(hasBankDetails
                    ? BookingCancellationStatus.REFUND_REVIEW_PENDING
                    : BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED);
        } else {
            log.error("Late PayOS payment needs manual review while refund is already in progress, bookingId={}, cancellationId={}",
                    booking.getId(), cancellation.getId());
        }

        cancellationRepository.save(cancellation);
        bookingSocketEmitter.emitRefundRequested(
                booking.getHotel() == null ? null : booking.getHotel().getId(),
                booking.getId(),
                updatedRefundAmount,
                cancellation.getStatus());
    }

    @Override
    @Transactional
    public PaymentResponse createVietQRPaymentLink(PaymentRequest request) throws Exception {
        Optional<Order> oderOpt = orderRepository.findById(request.getOrderId());
        if (oderOpt.isEmpty()) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        Order order = oderOpt.get();
        Booking booking = order.getBooking();
        if (booking != null && booking.getBookingStatus().isCancelled()) {
            throw new AppException(ErrorCode.ORDER_NOT_OPEN);
        }
        long now = Instant.now().getEpochSecond();
        long expiredAt = now + 5 * 60L;
        if (booking != null && booking.getBookingStatus() == BookingStatus.PENDING) {
            if (booking.getCreatedAt() == null) {
                throw new AppException(ErrorCode.ORDER_NOT_OPEN);
            }
            expiredAt = booking.getCreatedAt().plusMinutes(5)
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond();
            if (expiredAt <= now) {
                throw new AppException(ErrorCode.ORDER_NOT_OPEN);
            }
        }
        synchronizeBookingSurcharges(order);

        long amountToPay;
        if (request.getAmount() != null && request.getAmount() > 0) {
            amountToPay = request.getAmount();
            // Đồng bộ số tiền thanh toán thực tế với Order (bao gồm VAT, phụ thu từ Web)
            order.setTotalAmount(BigDecimal.valueOf(amountToPay));
            // Cho phép tạo QR thanh toán mới nếu đơn trước đó từng bị đóng hoặc đang test
            if (order.getOrderStatus() != OrderStatusType.OPEN || (order.getPaidAmount() != null && order.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)) {
                order.setPaidAmount(BigDecimal.ZERO);
                order.setOrderStatus(OrderStatusType.OPEN);
                order.setCloseDate(null);
            }
            orderRepository.save(order);
        } else {
            if (order.getOrderStatus() != OrderStatusType.OPEN) {
                throw new AppException(ErrorCode.ORDER_NOT_OPEN);
            }
            BigDecimal remainingAmount = order.getRemainingAmount();
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException(ErrorCode.ORDER_ALREADY_PAID);
            }
            amountToPay = remainingAmount.longValue();
        }

        // Sinh mã 6 chữ số (100000 - 999999), đảm bảo không trùng với mã đang tồn tại
        long uniqueOrderCode = generateUniqueSixDigitCode();

        // Lấy tên khách hàng không dấu ngắn gọn để ghép vào nội dung chuyển khoản
        String customerShortName = "";
        String customerFullName = "";
        try {
            if (order.getBooking() != null && order.getBooking().getCustomer() != null) {
                customerFullName = order.getBooking().getCustomer().getFullName();
                if (customerFullName != null && !customerFullName.isBlank()) {
                    String[] parts = customerFullName.trim().split("\\s+");
                    String mainName = parts[parts.length - 1];
                    customerShortName = java.text.Normalizer.normalize(mainName, java.text.Normalizer.Form.NFD)
                            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                            .replace("đ", "d").replace("Đ", "D")
                            .replaceAll("[^a-zA-Z0-9]", "")
                            .toUpperCase();
                }
            }
        } catch (Exception ignored) {
        }

        // Rút gọn mã hóa đơn: lấy HD + 5 ký tự cuối (ví dụ HD49880) để đảm bảo không vượt quá 25 ký tự PayOS
        String orderIdStr = order.getId();
        String shortOrderCode;
        if (orderIdStr.length() > 7) {
            shortOrderCode = "HD" + orderIdStr.substring(orderIdStr.length() - 5);
        } else {
            shortOrderCode = orderIdStr;
        }

        String paymentDescription;
        if (!customerShortName.isEmpty()) {
            paymentDescription = customerShortName + "_" + shortOrderCode + "_" + amountToPay;
        } else {
            paymentDescription = "HD_" + shortOrderCode + "_" + amountToPay;
        }
        // PayOS quy định độ dài description <= 25 ký tự
        if (paymentDescription.length() > 25) {
            paymentDescription = paymentDescription.substring(0, 25).trim();
        }

        log.info("[QR] Tạo orderCode={} cho order.id={}, amount={}, desc={}", uniqueOrderCode, order.getId(), amountToPay, paymentDescription);

        String returnUrl = paymentResultUrl("success", order.getId());
        String cancelUrl = paymentResultUrl("cancelled", order.getId());
        CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                .orderCode(uniqueOrderCode)
                .amount(amountToPay)
                .description(paymentDescription)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .expiredAt(expiredAt)
                .build();

        String checkoutUrl;
        String qrCodeUrl;
        Long linkExpiredAt = null;
        String paymentMessage;

        try {
            var checkoutData = payOS.paymentRequests().create(paymentRequest);
            checkoutUrl = checkoutData.getCheckoutUrl();
            qrCodeUrl = checkoutData.getQrCode();
            linkExpiredAt = expiredAt;
            paymentMessage = booking != null && booking.getBookingStatus() == BookingStatus.PENDING
                    ? "Tạo link PayOS thành công (hết hạn khi booking hết thời gian giữ)"
                    : "Tạo link PayOS thành công (có hiệu lực 5 phút)";

            if (order.getPaymentOrderCodes() == null) {
                order.setPaymentOrderCodes(new ArrayList<>());
            }
            order.getPaymentOrderCodes().add(uniqueOrderCode);
            orderRepository.save(order);
        } catch (Exception e) {
            log.error("[PayOS] Không thể tạo link thanh toán orderId={}, orderCode={}", order.getId(), uniqueOrderCode, e);
            throw e;
        }

        return PaymentResponse.builder()
                .error(0)
                .message(paymentMessage)
                .checkoutUrl(checkoutUrl)
                .qrCode(qrCodeUrl)
                .description(paymentDescription)
                .customerName(customerFullName)
                .orderId(order.getId())
                .expiredAt(linkExpiredAt)
                .build();
    }

    private String paymentResultUrl(String paymentStatus, String orderId) {
        String baseUrl = frontendUrl.replaceAll("/+$", "");
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/booking-list")
                .queryParam("paymentStatus", paymentStatus)
                .queryParam("orderId", orderId)
                .build()
                .encode()
                .toUriString();
    }

    private long generateUniqueSixDigitCode() {
        long code;
        int attempts = 0;
        do {
            // 100000 - 999999, luôn đúng 6 chữ số
            code = 100000L + (long) (Math.random() * 900000L);
            attempts++;
            if (attempts > 20) {
                throw new AppException(ErrorCode.PAYMENT_CODE_GENERATION_FAILED); // hoặc mã lỗi tương ứng bạn có
            }
        } while (orderRepository.existsByPaymentOrderCode(code));
        return code;
    }

    @Override
    @Transactional
    public WebhookResponse processPayOSWebhook(JsonNode webhookBody) throws Exception {
        log.info("[Webhook] Bắt đầu xử lý PayOS webhook");
        if (webhookBody == null) {
            throw new IllegalArgumentException("PayOS webhook body is missing");
        }

        WebhookData data = payOS.webhooks().verify(webhookBody);
        String envelopeCode = webhookBody.has("code") ? webhookBody.get("code").asText() : "";
        if (!"00".equals(envelopeCode) || data.getOrderCode() == null || data.getAmount() == null
                || data.getAmount() <= 0 || data.getPaymentLinkId() == null
                || data.getPaymentLinkId().isBlank() || data.getReference() == null
                || data.getReference().isBlank()) {
            log.warn("[Webhook] Bỏ qua dữ liệu PayOS không hợp lệ hoặc không phải giao dịch thành công");
            return WebhookResponse.builder().error(4).message("Dữ liệu webhook không hợp lệ").build();
        }
        if (!"00".equals(data.getCode())) {
            log.info("[Webhook] Bỏ qua giao dịch PayOS chưa thành công, orderCode={}, code={}",
                    data.getOrderCode(), data.getCode());
            return WebhookResponse.builder().error(0).message("Giao dịch chưa thành công").build();
        }

        long uniqueOrderCode = data.getOrderCode();
        log.info("[Webhook] orderCode nhận từ PayOS={}", uniqueOrderCode);

        Optional<Order> orderLookup = orderRepository.findByPaymentOrderCodesContaining(uniqueOrderCode);
        if (orderLookup.isEmpty()) {
            log.warn("[Webhook] KHÔNG TÌM THẤY order với paymentOrderCode={}", uniqueOrderCode);
            return WebhookResponse.builder().error(4).message("Dữ liệu webhook không hợp lệ hoặc không tìm thấy đơn hàng tương ứng trong hệ thống").build();
        }
        Booking linkedBooking = orderLookup.get().getBooking();
        if (linkedBooking != null) {
            bookingRepository.findByIdForUpdate(linkedBooking.getId());
        }
        Optional<Order> orderOpt = orderRepository.findByPaymentOrderCodeForUpdate(uniqueOrderCode);
        if (orderOpt.isEmpty()) {
            log.warn("[Webhook] order không còn liên kết với paymentOrderCode={}", uniqueOrderCode);
            return WebhookResponse.builder().error(4).message("Dữ liệu webhook không hợp lệ").build();
        }
        Order order = orderOpt.get();
        log.info("[Webhook] Tìm thấy order.id={}, orderStatus={}, paidAmount(trước)={}",
                order.getId(), order.getOrderStatus(), order.getPaidAmount());

        // Lấy các field còn lại từ dataNode - phần này bị thiếu ở bản trước
        long amountPaidOnGateway = data.getAmount();
        String counterAccountName = data.getCounterAccountName();
        String paymentLinkId = data.getPaymentLinkId();

        PaymentType xacDinhPaymentType = PaymentType.BANK;
        if (counterAccountName != null && (counterAccountName.toLowerCase().contains("momo") || counterAccountName.toLowerCase().contains("vnpay"))) {
            xacDinhPaymentType = PaymentType.EWALLET;
        }

        if (hasPayOSTransactionReference(order, data.getReference())) {
            log.info("[Webhook] Bỏ qua webhook đã được ghi nhận cho order.id={}, paymentLinkId={}",
                    order.getId(), paymentLinkId);
            return WebhookResponse.builder().error(0).message("Giao dịch đã được xử lý trước đó").build();
        }

        order.addPaymentSuccess(
                BigDecimal.valueOf(amountPaidOnGateway),
                xacDinhPaymentType,
                paymentLinkId,
                data.getReference()
        );
        reconcileLatePayment(order, amountPaidOnGateway);

        log.info("[Webhook] Sau addPaymentSuccess: paidAmount={}, orderStatus={}, số lượng paymentTransactions={}",
                order.getPaidAmount(), order.getOrderStatus(),
                order.getPaymentTransactions() != null ? order.getPaymentTransactions().size() : 0);

        orderRepository.save(order);
        Booking booking = order.getBooking();
        if (booking != null && booking.getBookingStatus() == BookingStatus.PENDING
                && order.getRemainingAmount().signum() == 0) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            if (booking.getBookingDetails() != null) {
                booking.getBookingDetails().forEach(detail -> {
                    if (detail.getStatus() == BookingStatusType.CANCELLED) {
                        detail.setStatus(BookingStatusType.PENDING);
                        detail.setCancelledAt(null);
                    }
                });
            }
            bookingRepository.save(booking);
        }
        log.info("[Webhook] Đã gọi orderRepository.save(order.id={})", order.getId());
        bookingSocketEmitter.emitPaymentReceived(order, BigDecimal.valueOf(amountPaidOnGateway), xacDinhPaymentType);
        sendPaymentEmailAfterCommit(
                order,
                BigDecimal.valueOf(amountPaidOnGateway),
                xacDinhPaymentType,
                paymentLinkId,
                LocalDateTime.now()
        );

        return WebhookResponse.builder().error(0).message("Cập nhật đơn hàng thành công via Webhook").build();
    }

    @Override
    @Transactional
    public PaymentTransactionResponse payWithCash(
            CashPaymentRequest request
    ) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() ->
                        new AppException(ErrorCode.ORDER_NOT_FOUND)
                );

        synchronizeBookingSurcharges(order);

        if (order.getOrderStatus() == OrderStatusType.CANCELLED) {
            throw new AppException(
                    ErrorCode.ORDER_ALREADY_CANCELLED
            );
        }

        if (order.getOrderStatus() == OrderStatusType.PAID
                || order.getOrderStatus() == OrderStatusType.CLOSED) {
            throw new AppException(
                    ErrorCode.ORDER_ALREADY_PAID
            );
        }

        if (request.getAmountPaid() == null
                || request.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(
                    ErrorCode.INVALID_PAYMENT_AMOUNT
            );
        }

        // Lấy số tiền thực tế còn lại cần thanh toán
        BigDecimal remainingAmount = order.getRemainingAmount(); // cho nay phai so sanh remain do la field nay luu tien chua thanh toan
        BigDecimal amountPaid = request.getAmountPaid();

        if (remainingAmount == null
                || remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(
                    ErrorCode.INVALID_PAYMENT_AMOUNT
            );
        }

        if (amountPaid.compareTo(remainingAmount) < 0) {
            throw new AppException(
                    ErrorCode.INSUFFICIENT_PAYMENT
            );
        }

        BigDecimal changeAmount =
                amountPaid.subtract(remainingAmount);

        PaymentTransaction receiptTransaction =
                PaymentTransaction.builder()
                        .order(order)
                        .amount(amountPaid)
                        .paymentType(PaymentType.CASH)
                        .cashFlowType(CashFlowType.RECEIPT)
                        .note(
                                request.getNote() != null
                                        ? request.getNote()
                                        : "Thanh toán tiền mặt tại quầy"
                        )
                        .transactionDate(LocalDateTime.now())
                        .build();

        paymentRepository.save(receiptTransaction);

        if (changeAmount.compareTo(BigDecimal.ZERO) > 0) {
            PaymentTransaction changeTransaction =
                    PaymentTransaction.builder()
                            .order(order)
                            .amount(changeAmount)
                            .paymentType(PaymentType.CASH)
                            .cashFlowType(CashFlowType.CHANGE)
                            .note("Tiền thừa trả khách")
                            .transactionDate(LocalDateTime.now())
                            .build();

            paymentRepository.save(changeTransaction);
        }

        // --- SỬA QUAN TRỌNG Ở ĐÂY ---
        // 1. Cộng dồn số tiền đã trả từ trước + số tiền thực trả lần này (trừ đi tiền thừa nếu có, hoặc dùng luôn remainingAmount vì khách đã trả đủ phần còn thiếu)
        BigDecimal currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        order.setPaidAmount(currentPaid.add(remainingAmount));

        // 2. Cập nhật số tiền còn lại về 0 vì đã trả hết phần thiếu
        order.setRemainingAmount(BigDecimal.ZERO);

        order.setRemainingAmount(BigDecimal.ZERO);
        order.setOrderStatus(OrderStatusType.OPEN);
        // thanh toán bằng tiền mặt cập nhật là thanh toán đủ lần ầu tiên
        order.setPaymentStatus(PaymentStatus.PAID);
        // cap nhat trang thai confirm
        if (order.getBooking() != null
                && order.getBooking().getBookingStatus() == BookingStatus.PENDING) {
            order.getBooking().setBookingStatus(BookingStatus.CONFIRMED);
        }

        orderRepository.save(order);
        bookingSocketEmitter.emitPaymentReceived(order, remainingAmount, PaymentType.CASH);
        sendPaymentEmailAfterCommit(
                order,
                remainingAmount,
                PaymentType.CASH,
                receiptTransaction.getId() != null ? receiptTransaction.getId().toString() : null,
                receiptTransaction.getTransactionDate()
        );

        return PaymentTransactionResponse.builder()
                .transactionId(receiptTransaction.getId())
                .orderId(order.getId())
                .totalAmount(remainingAmount)
                .amountPaid(amountPaid)
                .changeAmount(changeAmount)
                .paymentType("CASH")
                .cashFlowType(CashFlowType.RECEIPT)
                .transactionDate(
                        receiptTransaction.getTransactionDate()
                )
                .message("Thanh toán tiền mặt thành công")
                .build();
    }

    private void synchronizeBookingSurcharges(Order order) {
        Booking booking = order.getBooking();
        if (booking == null || booking.getBookingDetails() == null || booking.getBookingDetails().isEmpty()) {
            return;
        }

        BigDecimal detailSurcharges = booking.getBookingDetails().stream()
                .filter(detail -> detail.getStatus() != BookingStatusType.CANCELLED)
                .map(this::surchargesOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recordedSurcharges = order.getSurchargeTotalAmount() != null
                ? order.getSurchargeTotalAmount()
                : BigDecimal.ZERO;
        BigDecimal surchargeTotal = detailSurcharges.max(recordedSurcharges);

        BigDecimal roomTotal = order.getRoomTotalAmount() != null ? order.getRoomTotalAmount() : BigDecimal.ZERO;
        BigDecimal serviceTotal = order.getServiceTotalAmount() != null ? order.getServiceTotalAmount() : BigDecimal.ZERO;
        BigDecimal roomDiscount = order.getDiscountRoomAmount() != null ? order.getDiscountRoomAmount() : BigDecimal.ZERO;
        BigDecimal serviceDiscount = order.getDiscountServiceAmount() != null ? order.getDiscountServiceAmount() : BigDecimal.ZERO;
        BigDecimal totalDiscount = order.getDiscountAmountTotal() != null ? order.getDiscountAmountTotal() : BigDecimal.ZERO;
        BigDecimal calculatedTotal = roomTotal.add(serviceTotal).add(surchargeTotal)
                .subtract(roomDiscount).subtract(serviceDiscount).subtract(totalDiscount)
                .max(BigDecimal.ZERO);
        BigDecimal currentTotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal correctedTotal = currentTotal.max(calculatedTotal);

        BigDecimal paidAmount = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = correctedTotal.subtract(paidAmount).max(BigDecimal.ZERO);

        order.setSurchargeTotalAmount(surchargeTotal);
        order.setTotalAmount(correctedTotal);
        order.setRemainingAmount(remainingAmount);
        order.setPaymentStatus(remainingAmount.signum() == 0
                ? PaymentStatus.PAID
                : paidAmount.signum() > 0
                ? PaymentStatus.PARTIAL
                : PaymentStatus.UNPAID);

        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0
                && (order.getOrderStatus() == OrderStatusType.PAID
                || order.getOrderStatus() == OrderStatusType.CLOSED)) {
            order.setOrderStatus(OrderStatusType.OPEN);
            order.setCloseDate(null);
        }

        orderRepository.save(order);
    }

    private BigDecimal surchargesOf(BookingDetail detail) {
        BigDecimal early = detail.getEarlyCheckInFee() != null ? detail.getEarlyCheckInFee() : BigDecimal.ZERO;
        BigDecimal late = detail.getLateCheckOutFee() != null ? detail.getLateCheckOutFee() : BigDecimal.ZERO;
        BigDecimal other = detail.getOtherSurcharges() != null ? detail.getOtherSurcharges() : BigDecimal.ZERO;
        return early.add(late).add(other);
    }

    @Override
    @Transactional
    public Map<String, Object> checkOrderStatus(String orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId)
                .or(() -> orderRepository.findByBookingId(orderId));
        if (orderOpt.isEmpty()) {
            return Map.of("isPaid", false, "status", "NOT_FOUND", "message", "Không tìm thấy hóa đơn");
        }
        Order order = orderOpt.get();

        // 1. Nếu đơn hàng đã hoàn tất (CLOSED hoặc PAID)
        if (order.getOrderStatus() == OrderStatusType.CLOSED || order.getOrderStatus() == OrderStatusType.PAID) {
            Booking booking = order.getBooking();
            boolean bookingCancelled = booking != null && booking.getBookingStatus().isCancelled();
            return Map.of(
                    "isPaid", true,
                    "status", order.getOrderStatus().name(),
                    "paidAmount", order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO,
                    "bookingStatus", booking == null ? "" : booking.getBookingStatus().name(),
                    "message", bookingCancelled
                            ? "Đã nhận tiền nhưng booking đã hủy; yêu cầu hoàn tiền cần được xử lý"
                            : "Đơn hàng đã được thanh toán thành công"
            );
        }

        // 2. Tra cứu trực tiếp từ cổng PayOS qua danh sách paymentOrderCodes
        String gatewayStatus = "NOT_CREATED";
        boolean verificationUnavailable = false;
        if (order.getPaymentOrderCodes() != null && !order.getPaymentOrderCodes().isEmpty()) {
            for (Long orderCode : order.getPaymentOrderCodes()) {
                try {
                    PaymentLink link = payOS.paymentRequests().get(orderCode);
                    if (link != null) {
                        gatewayStatus = link.getStatus() == null ? "UNKNOWN" : link.getStatus().getValue();
                        log.info("[CheckPayOS] orderId={}, orderCode={}, status={}", order.getId(), orderCode, link.getStatus());
                        if (link.getStatus() == PaymentLinkStatus.PAID) {
                            long amountPaidOnLink = link.getAmountPaid() != null && link.getAmountPaid() > 0
                                    ? link.getAmountPaid()
                                    : link.getAmount();
                            long amountAlreadyRecorded = recordedPaymentAmountForLink(order, link.getId());
                            long amountToRecord = Math.max(0, amountPaidOnLink - amountAlreadyRecorded);
                            if (amountToRecord > 0) {
                                order.addPaymentSuccess(BigDecimal.valueOf(amountToRecord), PaymentType.BANK, link.getId());
                                reconcileLatePayment(order, amountToRecord);
                                bookingSocketEmitter.emitPaymentReceived(order, BigDecimal.valueOf(amountToRecord), PaymentType.BANK);
                                sendPaymentEmailAfterCommit(
                                        order,
                                        BigDecimal.valueOf(amountToRecord),
                                        PaymentType.BANK,
                                        link.getId(),
                                        LocalDateTime.now()
                                );
                            }
                            boolean fullyPaid = order.getRemainingAmount().signum() == 0;
                            Booking booking = order.getBooking();
                            if (fullyPaid) {
                                order.setOrderStatus(OrderStatusType.CLOSED);
                                order.setCloseDate(LocalDateTime.now());
                            }
                            if (fullyPaid && booking != null && booking.getBookingStatus() == BookingStatus.PENDING) {
                                booking.setBookingStatus(BookingStatus.CONFIRMED);
                                bookingRepository.save(booking);
                            }
                            orderRepository.save(order);

                            return Map.of(
                                    "isPaid", fullyPaid,
                                    "status", fullyPaid ? "PAID" : "PARTIAL",
                                    "paymentStatus", fullyPaid ? "PAID" : "PARTIAL",
                                    "paidAmount", order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO,
                                    "remainingAmount", order.getRemainingAmount(),
                                    "bookingStatus", booking == null ? "" : booking.getBookingStatus().name(),
                                    "gatewayStatus", fullyPaid ? "PAID" : "UNDERPAID",
                                    "message", fullyPaid
                                            ? "Xác nhận thanh toán thành công từ ngân hàng qua PayOS"
                                            : "Đã nhận một phần thanh toán, đơn hàng vẫn còn số dư"
                            );
                        }

                    }
                } catch (Exception e) {
                    verificationUnavailable = true;
                    log.warn("[CheckPayOS] Tra cứu orderCode={} trên PayOS gặp lỗi: {}", orderCode, e.getMessage());
                }
            }
        }

        return Map.of(
                "isPaid", false,
                "status", order.getOrderStatus() != null ? order.getOrderStatus().name() : "OPEN",
                "paymentStatus", order.getPaymentStatus() == null ? "UNPAID" : order.getPaymentStatus().name(),
                "gatewayStatus", verificationUnavailable ? "UNAVAILABLE" : gatewayStatus,
                "paidAmount", order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO,
                "remainingAmount", order.getRemainingAmount(),
                "message", verificationUnavailable
                        ? "Chưa thể xác minh trạng thái giao dịch với PayOS"
                        : "Đang chờ khách chuyển khoản"
        );
    }

    private boolean hasPayOSTransactionReference(Order order, String transactionReference) {
        String referenceMarker = " | PayOS Transaction Ref: " + transactionReference + " |";
        return order.getPaymentTransactions() != null
                && order.getPaymentTransactions().stream()
                .anyMatch(transaction -> transaction.getNote() != null
                        && transaction.getNote().contains(referenceMarker));
    }

    private long recordedPaymentAmountForLink(Order order, String paymentLinkId) {
        if (order.getPaymentTransactions() == null) {
            return 0;
        }
        String linkMarker = "PayOS Webhook Ref: " + paymentLinkId;
        return order.getPaymentTransactions().stream()
                .filter(transaction -> transaction.getNote() != null
                        && (transaction.getNote().equals(linkMarker)
                        || transaction.getNote().startsWith(linkMarker + " |")))
                .map(PaymentTransaction::getAmount)
                .filter(amount -> amount != null)
                .mapToLong(BigDecimal::longValue)
                .sum();
    }

    private void sendPaymentEmailAfterCommit(
            Order order,
            BigDecimal amount,
            PaymentType paymentType,
            String paymentReference,
            LocalDateTime paidAt) {
        Booking booking = order.getBooking();
        if (booking == null || booking.getCustomer() == null) {
            log.warn("Bỏ qua email thanh toán cho order {} vì không tìm thấy khách hàng", order.getId());
            return;
        }
        String email = emailService.getCustomerEmail(booking.getCustomer());
        if (email == null) {
            log.info("Bỏ qua email thanh toán cho booking {} vì khách hàng chưa có email", booking.getId());
            return;
        }

        String customerName = booking.getCustomer().getFullName();
        String bookingId = booking.getId();
        String orderId = order.getId();
        String hotelName = booking.getHotel() != null ? booking.getHotel().getName() : "Khách sạn";
        BigDecimal paidAmount = order.getPaidAmount();
        BigDecimal remainingAmount = order.getRemainingAmount();
        String paymentTypeName = paymentType != null ? paymentType.name() : "N/A";

        emailService.sendAfterCommit("payment for booking " + bookingId, () ->
                emailService.sendPaymentReceivedEmail(
                        email,
                        customerName,
                        bookingId,
                        orderId,
                        hotelName,
                        amount,
                        paidAmount,
                        remainingAmount,
                        paymentTypeName,
                        paymentReference,
                        paidAt
                ));
    }

    @Override
    @Transactional
    public void cancelOutstandingPaymentLinks(String bookingId) {
        Optional<Order> orderOpt = orderRepository.findByBookingIdForUpdate(bookingId);
        if (orderOpt.isEmpty()) {
            return;
        }
        Order order = orderOpt.get();
        if (order.getPaymentOrderCodes() == null || order.getPaymentOrderCodes().isEmpty()) {
            return;
        }

        for (Long orderCode : order.getPaymentOrderCodes()) {
            PaymentLink link = payOS.paymentRequests().get(orderCode);
            if (link == null) {
                continue;
            }
            PaymentLinkStatus status = link.getStatus();
            if (status == PaymentLinkStatus.PAID || status == PaymentLinkStatus.UNDERPAID) {
                String paymentLinkId = link.getId();
                long amountPaid = link.getAmountPaid() != null && link.getAmountPaid() > 0
                        ? link.getAmountPaid()
                        : status == PaymentLinkStatus.PAID ? link.getAmount() : 0;
                long amountToRecord = Math.max(0, amountPaid - recordedPaymentAmountForLink(order, paymentLinkId));
                if (amountToRecord > 0) {
                    order.addPaymentSuccess(
                            BigDecimal.valueOf(amountToRecord),
                            PaymentType.BANK,
                            paymentLinkId);
                    order.setPaymentStatus(order.getRemainingAmount().signum() == 0
                            ? PaymentStatus.PAID
                            : PaymentStatus.PARTIAL);
                }
            }
            if (status == PaymentLinkStatus.PENDING || status == PaymentLinkStatus.UNDERPAID
                    || status == PaymentLinkStatus.PROCESSING) {
                payOS.paymentRequests().cancel(orderCode, "Booking cancellation requested");
            }
        }
        orderRepository.save(order);
    }

}
