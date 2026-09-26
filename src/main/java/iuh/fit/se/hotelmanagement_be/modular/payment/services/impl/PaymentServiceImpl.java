package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.PaymentTransaction;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
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
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    // Tự viết Constructor tường minh để khởi tạo đầy đủ các bean và thông tin cấu hình PayOS
    public PaymentServiceImpl(
            OrderRepository orderRepository,
            ObjectMapper objectMapper,
            @Value("${CLIENT_ID}") String clientId,
            @Value("${API_KEY}") String apiKey,
            @Value("${CHECKSUM_KEY}") String checksumKey,
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository
    ) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.paymentRepository = paymentRepository;
        this.payOS = new PayOS(clientId, apiKey, checksumKey);
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional
    public PaymentResponse createVietQRPaymentLink(PaymentRequest request) throws Exception {
        Optional<Order> oderOpt = orderRepository.findById(request.getOrderId());
        if (oderOpt.isEmpty()) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        Order order = oderOpt.get();

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
        } catch (Exception ignored) {}

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

        CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                .orderCode(uniqueOrderCode)
                .amount(amountToPay)
                .description(paymentDescription)
                .returnUrl("http://localhost:8080/bookings?status=success&orderId=" + order.getId())
                .cancelUrl("http://localhost:8080/bookings?status=cancel&orderId=" + order.getId())
                .build();

        String checkoutUrl;
        String qrCodeUrl;

        try {
            var checkoutData = payOS.paymentRequests().create(paymentRequest);
            checkoutUrl = checkoutData.getCheckoutUrl();
            qrCodeUrl = checkoutData.getQrCode();

            if (order.getPaymentOrderCodes() == null) {
                order.setPaymentOrderCodes(new ArrayList<>());
            }
            order.getPaymentOrderCodes().add(uniqueOrderCode);
            orderRepository.save(order);
        } catch (Exception e) {
            log.warn("[PayOS] Cổng PayOS chưa sẵn sàng ({}), sinh mã VietQR chuẩn dự phòng", e.getMessage());
            // Sinh link ảnh VietQR chuẩn quét bằng mọi app ngân hàng
            qrCodeUrl = String.format("https://img.vietqr.io/image/MB-0345240759-compact2.png?amount=%d&addInfo=%s&accountName=SEN+VIET+HOTEL",
                    amountToPay, paymentDescription.replace(" ", "+"));
            checkoutUrl = qrCodeUrl;
        }

        return PaymentResponse.builder()
                .error(0)
                .message("Sinh mã VietQR thành công (Hạn 5 phút)")
                .checkoutUrl(checkoutUrl)
                .qrCode(qrCodeUrl)
                .description(paymentDescription)
                .customerName(customerFullName)
                .orderId(order.getId())
                .build();
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
        log.info("[Webhook] Bắt đầu xử lý: {}", webhookBody);

        if (webhookBody == null || !webhookBody.has("data")) {
            log.warn("[Webhook] Thiếu field 'data' trong payload -> có thể là ping test từ PayOS dashboard");
            return WebhookResponse.builder().error(4).message("Dữ liệu webhook trống").build();
        }

        JsonNode dataNode = webhookBody.get("data");
        String code = webhookBody.has("code") ? webhookBody.get("code").asText() : "";
        log.info("[Webhook] code={}", code);

        if (dataNode == null || !"00".equals(code)) {
            log.warn("[Webhook] code khác '00' hoặc dataNode null -> KHÔNG xử lý cập nhật đơn. code={}, dataNode={}", code, dataNode);
            return WebhookResponse.builder().error(4).message("Dữ liệu webhook không hợp lệ hoặc không tìm thấy đơn hàng tương ứng trong hệ thống").build();
        }

        // Tra order bằng paymentOrderCode đã lưu lúc tạo QR - không suy luận ngược nữa
        long uniqueOrderCode = dataNode.get("orderCode").asLong();
        log.info("[Webhook] orderCode nhận từ PayOS={}", uniqueOrderCode);

        Optional<Order> orderOpt = orderRepository.findByPaymentOrderCodesContaining(uniqueOrderCode);
        if (orderOpt.isEmpty()) {
            log.warn("[Webhook] KHÔNG TÌM THẤY order với paymentOrderCode={}", uniqueOrderCode);
            return WebhookResponse.builder().error(4).message("Dữ liệu webhook không hợp lệ hoặc không tìm thấy đơn hàng tương ứng trong hệ thống").build();
        }
        Order order = orderOpt.get();
        log.info("[Webhook] Tìm thấy order.id={}, orderStatus={}, paidAmount(trước)={}",
                order.getId(), order.getOrderStatus(), order.getPaidAmount());

        // Lấy các field còn lại từ dataNode - phần này bị thiếu ở bản trước
        long amountPaidOnGateway = dataNode.get("amount").asLong();
        String counterAccountName = dataNode.has("counterAccountName") && !dataNode.get("counterAccountName").isNull()
                ? dataNode.get("counterAccountName").asText() : null;
        String paymentLinkId = dataNode.has("paymentLinkId") ? dataNode.get("paymentLinkId").asText() : "";

        PaymentType xacDinhPaymentType = PaymentType.BANK;
        if (counterAccountName != null && (counterAccountName.toLowerCase().contains("momo") || counterAccountName.toLowerCase().contains("vnpay"))) {
            xacDinhPaymentType = PaymentType.EWALLET;
        }

        order.addPaymentSuccess(
                BigDecimal.valueOf(amountPaidOnGateway),
                xacDinhPaymentType,
                paymentLinkId
        );

        log.info("[Webhook] Sau addPaymentSuccess: paidAmount={}, orderStatus={}, số lượng paymentTransactions={}",
                order.getPaidAmount(), order.getOrderStatus(),
                order.getPaymentTransactions() != null ? order.getPaymentTransactions().size() : 0);

        orderRepository.save(order);
        log.info("[Webhook] Đã gọi orderRepository.save(order.id={})", order.getId());

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

        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal amountPaid = request.getAmountPaid();

        if (totalAmount == null
                || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(
                    ErrorCode.INVALID_PAYMENT_AMOUNT
            );
        }

        if (amountPaid.compareTo(totalAmount) < 0) {
            throw new AppException(
                    ErrorCode.INSUFFICIENT_PAYMENT
            );
        }

        BigDecimal changeAmount =
                amountPaid.subtract(totalAmount);

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

        // Quan trọng: cập nhật số tiền đã thanh toán
        order.setPaidAmount(totalAmount);
        order.setOrderStatus(OrderStatusType.PAID);
        // cap nhat trang thai confirm
        order.getBooking().setBookingStatus(BookingStatus.CONFIRMED);
        orderRepository.save(order);

        return PaymentTransactionResponse.builder()
                .transactionId(receiptTransaction.getId())
                .orderId(order.getId())
                .totalAmount(totalAmount)
                .amountPaid(amountPaid)
                .changeAmount(changeAmount)
                .paymentType("CASH")
                .status(OrderStatusType.PAID.name())
                .transactionDate(
                        receiptTransaction.getTransactionDate()
                )
                .message("Thanh toán tiền mặt thành công")
                .build();
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
            return Map.of(
                    "isPaid", true,
                    "status", order.getOrderStatus().name(),
                    "paidAmount", order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO,
                    "message", "Đơn hàng đã được thanh toán thành công"
            );
        }

        // 2. Tra cứu trực tiếp từ cổng PayOS qua danh sách paymentOrderCodes
        if (order.getPaymentOrderCodes() != null && !order.getPaymentOrderCodes().isEmpty()) {
            for (Long orderCode : order.getPaymentOrderCodes()) {
                try {
                    PaymentLink link = payOS.paymentRequests().get(orderCode);
                    if (link != null) {
                        log.info("[CheckPayOS] orderId={}, orderCode={}, status={}", order.getId(), orderCode, link.getStatus());
                        if (link.getStatus() == PaymentLinkStatus.PAID) {
                            long amountPaid = link.getAmountPaid() != null && link.getAmountPaid() > 0 ? link.getAmountPaid() : link.getAmount();
                            order.addPaymentSuccess(BigDecimal.valueOf(amountPaid), PaymentType.BANK, link.getId());
                            order.setOrderStatus(OrderStatusType.CLOSED);
                            order.setCloseDate(LocalDateTime.now());

                            Booking booking = order.getBooking();
                            if (booking != null) {
                                booking.setBookingStatus(BookingStatus.CONFIRMED);
                                bookingRepository.save(booking);
                            }
                            orderRepository.save(order);

                            return Map.of(
                                    "isPaid", true,
                                    "status", "PAID",
                                    "paidAmount", amountPaid,
                                    "message", "Xác nhận thanh toán thành công từ ngân hàng qua PayOS"
                            );
                        }
                    }
                } catch (Exception e) {
                    log.warn("[CheckPayOS] Tra cứu orderCode={} trên PayOS gặp lỗi: {}", orderCode, e.getMessage());
                }
            }
        }

        return Map.of(
                "isPaid", false,
                "status", order.getOrderStatus() != null ? order.getOrderStatus().name() : "OPEN",
                "remainingAmount", order.getRemainingAmount(),
                "message", "Đang chờ khách chuyển khoản"
        );
    }
}
