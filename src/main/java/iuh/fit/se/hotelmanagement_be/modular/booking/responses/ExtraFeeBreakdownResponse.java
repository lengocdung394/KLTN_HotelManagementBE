package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtraFeeBreakdownResponse {
    double adultExtraFee;   // Tiền phụ thu người lớn
    double childExtraFee;   // Tiền phụ thu trẻ em
    double totalExtraFee;   // Tổng tiền phụ thu
}
