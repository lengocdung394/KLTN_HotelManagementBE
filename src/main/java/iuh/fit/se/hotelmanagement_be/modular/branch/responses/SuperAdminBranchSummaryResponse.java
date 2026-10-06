package iuh.fit.se.hotelmanagement_be.modular.branch.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminBranchSummaryResponse {
    Long id;
    String name;
    String address;
    String phone;
    String provinceName;
    long employeeCount;
    long bookingCount;
    BigDecimal totalRevenue;
}
