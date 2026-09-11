package iuh.fit.se.hotelmanagement_be.modular.room.responses;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BedTypeGetAllResponse {
    Long id;
    String name; // Ví dụ: "Single Bed", "Queen Bed", "King Bed", "Extra Bed (Giường phụ)"
    String description; // Sửa lỗi chính tả từ discription thành description nhé bạn

}
