package iuh.fit.se.hotelmanagement_be.modular.room.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "bed_types")
public class BedType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotBlank(message = "Tên loại giường không được để trống")
    @Column(name = "name", nullable = false, unique = true)
    String name; // Ví dụ: "Single Bed", "Queen Bed", "King Bed", "Extra Bed (Giường phụ)"

    @Column(name = "description")
    String description; // Sửa lỗi chính tả từ discription thành description nhé bạn

    @NotNull(message = "Sức chứa tiêu chuẩn của giường không được để trống")
    @Min(value = 1, message = "Sức chứa tối thiểu là 1 người")
    @Column(name = "capacity", nullable = false)
    Integer capacity; // Ví dụ: Giường Single = 1, Giường Queen/King = 2

    @NotNull(message = "Phải xác định đây có phải giường phụ hay không")
    @Column(name = "is_extra_bed", nullable = false)
    Boolean isExtraBed; // true nếu là giường phụ (Extra Bed/Sofa Bed), false nếu là giường mặc định của phòng
}