package iuh.fit.se.hotelmanagement_be.modular.room.responses.responseForExcelRoom;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public  class ImportDetailItem {
    int rowNumber;     // Dòng số mấy trong Excel
    String roomNumber; // Số phòng
    String result;     // SUCCESS hoặc FAILED
    String reason;     // Lý do nếu lỗi (Ví dụ: "Thiếu ảnh", "Không tìm thấy tầng",...)
}