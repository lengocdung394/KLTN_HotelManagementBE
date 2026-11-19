package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmployeeExcelRowRequest {
    String fullName;
    String email;
    String phone;
    String cccd;
    String address;
    String position;
    String avatarUrl;
}
