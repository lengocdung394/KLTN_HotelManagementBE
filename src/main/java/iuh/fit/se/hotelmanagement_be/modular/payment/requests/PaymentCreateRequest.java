package iuh.fit.se.hotelmanagement_be.modular.payment.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Yêu cầu thực hiện thanh toán cho đơn / hóa đơn")
public class PaymentCreateRequest {

    @NotNull(message = "Mã hóa đơn không được để trống")
    @Schema(description = "ID của hóa đơn (Order ID)", example = "1")
    Long orderId;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền thanh toán phải lớn hơn 0")
    @Schema(description = "Số tiền thanh toán", example = "500000.00")
    BigDecimal amount;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    @Schema(description = "Phương thức thanh toán: CASH, BANK, EWALLET", example = "CASH")
    PaymentType paymentType;

    @Builder.Default
    @Schema(description = "Loại dòng tiền: RECEIPT (thu tiền), REFUND (hoàn tiền), CHANGE (tiền thừa trả lại)", example = "RECEIPT")
    CashFlowType cashFlowType = CashFlowType.RECEIPT;

    @Schema(description = "Ghi chú thanh toán", example = "Khách đặt cọc tiền phòng")
    String note;
}
