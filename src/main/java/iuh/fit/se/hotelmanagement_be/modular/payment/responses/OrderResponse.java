package iuh.fit.se.hotelmanagement_be.modular.payment.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Thông tin chi tiết hóa đơn thanh toán")
public class OrderResponse {
    @Schema(description = "Mã hóa đơn", example = "1")
    Long id;

    @Schema(description = "Mã đơn đặt phòng liên kết", example = "101")
    Long bookingId;

    @Schema(description = "ID khách hàng", example = "1")
    Long customerId;

    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn A")
    String customerName;

    @Schema(description = "Số điện thoại khách hàng", example = "0901234567")
    String customerPhone;

    @Schema(description = "Ngày lập hóa đơn")
    LocalDateTime issueDate;

    @Schema(description = "Ngày đóng / quyết toán hóa đơn")
    LocalDateTime closeDate;

    @Schema(description = "Trạng thái hóa đơn: OPEN, CLOSED, CANCELLED")
    OrderStatusType orderStatus;

    @Schema(description = "Tổng tiền phòng")
    BigDecimal roomTotalAmount;

    @Schema(description = "Tổng tiền dịch vụ")
    BigDecimal serviceTotalAmount;

    @Schema(description = "Tiền giảm giá tiền phòng")
    BigDecimal discountRoomAmount;

    @Schema(description = "Tiền giảm giá dịch vụ")
    BigDecimal discountServiceAmount;

    @Schema(description = "Tổng số tiền giảm giá")
    BigDecimal discountAmountTotal;

    @Schema(description = "Tổng số tiền phải thanh toán sau khi trừ giảm giá")
    BigDecimal totalAmount;

    @Schema(description = "Số tiền đã thanh toán")
    BigDecimal paidAmount;

    @Schema(description = "Số tiền còn lại cần thanh toán")
    BigDecimal remainingAmount;

    @Schema(description = "Danh sách các lần thanh toán của hóa đơn")
    List<PaymentTransactionResponse> paymentTransactions;
}
