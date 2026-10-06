package iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForExcel;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomExcelImportRequest {
    List<RoomExcelRawRequest> rooms;
}
