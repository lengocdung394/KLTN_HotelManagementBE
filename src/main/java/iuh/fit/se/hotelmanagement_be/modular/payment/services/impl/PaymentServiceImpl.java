package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.OrderRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.PaymentRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.PaymentResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.WebhookResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;

import java.math.BigDecimal;
import java.util.Optional;
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final PayOS payOS;

    // Tự viết Constructor tường minh để khởi tạo đầy đủ các bean và thông tin cấu hình PayOS
    public PaymentServiceImpl(
            OrderRepository orderRepository,
            ObjectMapper objectMapper,
            @Value("${CLIENT_ID}") String clientId,
            @Value("${API_KEY}") String apiKey,
            @Value("${CHECKSUM_KEY}") String checksumKey
    ) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.payOS = new PayOS(clientId, apiKey, checksumKey);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public PaymentResponse createVietQRPaymentLink(PaymentRequest request) throws Exception {
//        Optional<Order> oderOpt = orderRepository.findById(request.getOrderId());
//        if (oderOpt.isEmpty()) {
//            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
//        }
//        Order order = oderOpt.get();
//        if (order.getOrderStatus() != OrderStatusType.OPEN) {
//            throw new AppException(ErrorCode.ORDER_NOT_OPEN);
//        }
//
//        BigDecimal remainingAmount = order.getRemainingAmount();
//        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new AppException(ErrorCode.ORDER_ALREADY_PAID);
//        }
//
//        long amountToPay = remainingAmount.longValue();
//        // Tạo orderCode độc nhất bằng cách kết hợp ID đơn hàng và timestamp để tránh bị trùng trên PayOS khi test nhiều lần
//        long uniqueOrderCode = order.getId() * 1000L + (System.currentTimeMillis() % 1000);
//        String paymentDescription = "Thanh toan HD" + order.getId();
//
//        // CHỈNH SỬA: Sử dụng CreatePaymentLinkRequest chuẩn V2 thay cho class cũ
//        CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
//                .orderCode(uniqueOrderCode)
//                .amount(amountToPay)
//                .description(paymentDescription)
//                .returnUrl("http://localhost:8080/bookings?status=success&orderId=" + order.getId())
//                .cancelUrl("http://localhost:8080/bookings?status=cancel&orderId=" + order.getId())
//                .build();
//
//        // CHỈNH SỬA: Sử dụng var và gọi hàm dạng module .paymentRequests().create()
//        var checkoutData = payOS.paymentRequests().create(paymentRequest);
//
//        return PaymentResponse.builder()
//                .error(0)
//                .message("Sinh mã VietQR thành công (Hạn 5 phút)")
//                .checkoutUrl(checkoutData.getCheckoutUrl())
//                .build();
//    }
//
//    @Override
//    public WebhookResponse processPayOSWebhook(JsonNode webhookBody) throws Exception {
//        Webhook webhookRaw = objectMapper.treeToValue(webhookBody, Webhook.class);
//
//        // CHỈNH SỬA: Sử dụng var và gọi hàm dạng module .webhooks().verify() chuẩn V2
//        var verifiedData = payOS.webhooks().verify(webhookRaw);
//
//        if (verifiedData != null && "00".equals(verifiedData.getCode())) {
//            Long orderId = verifiedData.getOrderCode();
//            long amountPaidOnGateway = verifiedData.getAmount();
//            String counterAccountName = verifiedData.getCounterAccountName();
//            Optional<Order> orderOpt = orderRepository.findById(orderId);
//            if (orderOpt.isPresent()) {
//                Order order = orderOpt.get();
//
//                PaymentType xacDinhPaymentType = PaymentType.BANK;
//                if (counterAccountName != null && (counterAccountName.toLowerCase().contains("momo") || counterAccountName.toLowerCase().contains("vnpay"))) {
//                    xacDinhPaymentType = PaymentType.EWALLET;
//                }
//
//                PaymentTransaction transaction = PaymentTransaction.builder()
//                        .amount(BigDecimal.valueOf(amountPaidOnGateway))
//                        .transactionDate(LocalDateTime.now())
//                        .paymentType(xacDinhPaymentType)
//                        .cashFlowType(CashFlowType.RECEIPT)
//                        .note("PayOS Webhook Ref: " + verifiedData.getPaymentLinkId())
//                        .order(order)
//                        .build();
//
//                if (order.getPaymentTransactions() == null) {
//                    order.setPaymentTransactions(new ArrayList<>());
//                }
//                order.getPaymentTransactions().add(transaction);
//                BigDecimal currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
//                order.setPaidAmount(currentPaid.add(BigDecimal.valueOf(amountPaidOnGateway)));
//
//                order.operation(); // Kích hoạt logic cốt lõi đóng đơn của bạn
//
//                orderRepository.save(order);
//
//                return WebhookResponse.builder().error(0).message("Cập nhật đơn hàng thành công via Webhook").build();
//            }
//        }
//        return WebhookResponse.builder().error(4).message("Dữ liệu webhook không hợp lệ hoặc không thành công").build();
//    }

    //
//    @Override
//    @Transactional // BẮT BUỘC: Thêm annotation này để Spring Boot tự động lưu các bảng Entity liên kết (PaymentTransaction) xuống DB
//    public WebhookResponse processPayOSWebhook(JsonNode webhookBody) throws Exception {
//        // 1. Kiểm tra an toàn dữ liệu đầu vào
//        if (webhookBody == null || !webhookBody.has("data")) {
//            return WebhookResponse.builder().error(4).message("Dữ liệu webhook trống").build();
//        }
//
//        // 2. Trích xuất trực tiếp dữ liệu từ JsonNode để tránh lỗi ép kiểu của Jackson
//        JsonNode dataNode = webhookBody.get("data");
//        String code = webhookBody.has("code") ? webhookBody.get("code").asText() : "";
//
//        if (dataNode != null && "00".equals(code)) {
//            long uniqueOrderCode = dataNode.get("orderCode").asLong();
//
//            // 3. GIẢI MÃ THÔNG MINH:
//            // Nếu mã đơn < 100000 (đơn cũ hoặc test tay), giữ nguyên làm ID.
//            // Nếu mã đơn >= 100000 (đơn hỗn hợp mới dạng ID * 1000 + đuôi), chia cho 1000L để lấy ID gốc.
//            Long orderId = (uniqueOrderCode < 100000L) ? uniqueOrderCode : (uniqueOrderCode / 1000L);
//
//            long amountPaidOnGateway = dataNode.get("amount").asLong();
//            String counterAccountName = dataNode.has("counterAccountName") && !dataNode.get("counterAccountName").isNull()
//                    ? dataNode.get("counterAccountName").asText() : null;
//            String paymentLinkId = dataNode.has("paymentLinkId") ? dataNode.get("paymentLinkId").asText() : "";
//
//            // 4. Tìm kiếm đơn hàng trong Database theo ID gốc chuẩn xác
//            Optional<Order> orderOpt = orderRepository.findById(orderId);
//            if (orderOpt.isPresent()) {
//                Order order = orderOpt.get();
//
//                // Xác định hình thức thanh toán (Ngân hàng hay Ví điện tử)
//                PaymentType xacDinhPaymentType = PaymentType.BANK;
//                if (counterAccountName != null && (counterAccountName.toLowerCase().contains("momo") || counterAccountName.toLowerCase().contains("vnpay"))) {
//                    xacDinhPaymentType = PaymentType.EWALLET;
//                }
//
//                // 5. Khởi tạo bản ghi lịch sử giao dịch (PaymentTransaction)
//                PaymentTransaction transaction = PaymentTransaction.builder()
//                        .amount(BigDecimal.valueOf(amountPaidOnGateway))
//                        .transactionDate(LocalDateTime.now())
//                        .paymentType(xacDinhPaymentType)
//                        .cashFlowType(CashFlowType.RECEIPT)
//                        .note("PayOS Webhook Ref: " + paymentLinkId)
//                        .order(order)
//                        .build();
//
//                if (order.getPaymentTransactions() == null) {
//                    order.setPaymentTransactions(new ArrayList<>());
//                }
//                order.getPaymentTransactions().add(transaction);
//
//                // 6. Cộng dồn số tiền thực tế khách đã chuyển khoản (Lưu đúng số tiền kể cả khi khách chuyển dư)
//                BigDecimal currentPaid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
//                order.setPaidAmount(currentPaid.add(BigDecimal.valueOf(amountPaidOnGateway)));
//
//                order.operation(); // Kích hoạt logic cốt lõi đóng đơn của bạn
//
//                // 7. Cập nhật thực thể xuống Database
//                orderRepository.save(order);
//
//                return WebhookResponse.builder().error(0).message("Cập nhật đơn hàng thành công via Webhook").build();
//            }
//        }
//        return WebhookResponse.builder().error(4).message("Dữ liệu webhook không hợp lệ hoặc không tìm thấy đơn hàng tương ứng trong hệ thống").build();
//    }
    @Override
    @Transactional
    public PaymentResponse createVietQRPaymentLink(PaymentRequest request) throws Exception {
        Optional<Order> oderOpt = orderRepository.findById(request.getOrderId());
        if (oderOpt.isEmpty()) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        Order order = oderOpt.get();
        if (order.getOrderStatus() != OrderStatusType.OPEN) {
            throw new AppException(ErrorCode.ORDER_NOT_OPEN);
        }

        BigDecimal remainingAmount = order.getRemainingAmount();
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.ORDER_ALREADY_PAID);
        }

        long amountToPay = remainingAmount.longValue();

        // Sinh mã 6 chữ số (100000 - 999999), đảm bảo không trùng với mã đang tồn tại
        long uniqueOrderCode = generateUniqueSixDigitCode();

        String paymentDescription = "Thanh toan HD" + order.getId();

        log.info("[QR] Tạo orderCode={} cho order.id={}, amount={}", uniqueOrderCode, order.getId(), amountToPay);

        CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                .orderCode(uniqueOrderCode)
                .amount(amountToPay)
                .description(paymentDescription)
                .returnUrl("http://localhost:8080/bookings?status=success&orderId=" + order.getId())
                .cancelUrl("http://localhost:8080/bookings?status=cancel&orderId=" + order.getId())
                .build();

        var checkoutData = payOS.paymentRequests().create(paymentRequest);

        // Lưu lại mã để lúc webhook về tra ngược chính xác, không cần đoán
        order.setPaymentOrderCode(uniqueOrderCode);
        orderRepository.save(order);

        return PaymentResponse.builder()
                .error(0)
                .message("Sinh mã VietQR thành công (Hạn 5 phút)")
                .checkoutUrl(checkoutData.getCheckoutUrl())
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

        Optional<Order> orderOpt = orderRepository.findByPaymentOrderCode(uniqueOrderCode);
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
}
