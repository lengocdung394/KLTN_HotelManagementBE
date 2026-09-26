package iuh.fit.se.hotelmanagement_be.modular.payment.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    int error;
    String message;
    String checkoutUrl; // mã QR hoặc link PayOS
    String qrCode;     // URL ảnh QR
    String description;
    String customerName;
    String orderId;
}
