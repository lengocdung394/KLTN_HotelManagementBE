package iuh.fit.se.hotelmanagement_be.modular.branch.requests;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminCreateProvinceRequest {
    String name;
    String backgroundImageUrl;
}
