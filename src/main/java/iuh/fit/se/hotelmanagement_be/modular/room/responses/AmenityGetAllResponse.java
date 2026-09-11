package iuh.fit.se.hotelmanagement_be.modular.room.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AmenityGetAllResponse {
    Long id;
    String name;
    Double price;
}
