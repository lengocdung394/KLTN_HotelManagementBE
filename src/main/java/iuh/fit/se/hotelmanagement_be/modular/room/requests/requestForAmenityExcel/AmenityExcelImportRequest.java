package iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForAmenityExcel;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AmenityExcelImportRequest {
    List<AmenityExcelRawRequest> amenities;
}
