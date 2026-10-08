package iuh.fit.se.hotelmanagement_be.modular.branch.responses;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminProvinceResponse {
    String id;
    String name;
    String backgroundImageUrl;
    List<SuperAdminHotelResponse> hotels;
}
