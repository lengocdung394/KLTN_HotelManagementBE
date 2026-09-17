package iuh.fit.se.hotelmanagement_be.modular.payment.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.PaymentRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.PaymentResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.WebhookResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment", description = "APIs liên quan đến thanh toán")
@Slf4j
public class PaymentController {
    PaymentService paymentService;
    ObjectMapper objectMapper;

    @Operation(summary = "Tạo link/mã VietQR thanh toán đơn hàng")
    @PostMapping("/create-qr")
    public ResponseEntity<PaymentResponse> createPaymentLink(@RequestBody PaymentRequest request) throws Exception {
        log.info("[Payment] Yêu cầu tạo QR cho orderId={}", request.getOrderId());
        PaymentResponse response = paymentService.createVietQRPaymentLink(request);
        log.info("[Payment] Tạo QR thành công, checkoutUrl={}", response.getCheckoutUrl());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Nhận Webhook từ PayOS khi khách thanh toán thành công")
    @PostMapping("/webhook/payos")
    public ResponseEntity<WebhookResponse> handlePayOSWebhook(@RequestBody Map<String, Object> webhookBody) {
        // Log RAW payload trước tiên - quan trọng nhất để biết PayOS thực sự gửi gì
        log.info("[Webhook] Raw payload nhận từ PayOS: {}", webhookBody);

        try {
            JsonNode jsonNode = objectMapper.valueToTree(webhookBody);
            WebhookResponse response = paymentService.processPayOSWebhook(jsonNode);

            log.info("[Webhook] Kết quả xử lý: error={}, message={}", response.getError(), response.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log đầy đủ stack trace - trước đây exception chỉ nằm trong response, không vào log server
            log.error("[Webhook] Lỗi xử lý webhook PayOS", e);
            return ResponseEntity.status(500).body(
                    WebhookResponse.builder()
                            .error(1)
                            .message("Lỗi xử lý webhook: " + e.getMessage())
                            .build()
            );
        }
    }

}
