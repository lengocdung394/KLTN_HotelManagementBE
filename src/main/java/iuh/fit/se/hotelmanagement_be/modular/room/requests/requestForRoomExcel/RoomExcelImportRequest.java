package iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForRoomExcel;

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
