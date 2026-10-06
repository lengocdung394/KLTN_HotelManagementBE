package iuh.fit.se.hotelmanagement_be.modular.service.requests.requestForExcel;

import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceExcelRequest {

    String name;          // Cột 0: Tên dịch vụ
    String description;   // Cột 1: Mô tả dịch vụ
    Double price;     // Cột 2: Giá tiền
    String unit;          // Cột 3: Đơn vị tính (Ví dụ: Lần, Suýt, Giờ...)
    String category;      // Cột 4: Danh mục (Ví dụ: Ăn uống, Spa, Giặt ủi...)
    String imageFileName; // Cột 5: Tên file ảnh khớp với trong thư mục images/ của file ZIP
}
