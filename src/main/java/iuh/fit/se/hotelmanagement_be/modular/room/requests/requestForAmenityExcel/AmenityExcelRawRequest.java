package iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForAmenityExcel;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AmenityExcelRawRequest {
    Integer row;
    String name;
    Double price;
}
