package iuh.fit.se.hotelmanagement_be.modular.room.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
@Table(
        name = "room_beds",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"room_id", "bed_type_id"}) // 1 phòng không bị lặp lại cùng 1 loại giường
        }
)
public class RoomBed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Thay vì trỏ vào RoomType, ta trỏ trực tiếp vào Room cụ thể
    @NotNull(message = "Phòng không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    Room room;

    @NotNull(message = "Loại giường không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bed_type_id", nullable = false)
    BedType bedType;

    @NotNull(message = "Số lượng giường không được để trống")
    @Min(value = 1, message = "Số lượng giường tối thiểu phải là 1")
    @Column(name = "quantity", nullable = false)
    Integer quantity; // Ví dụ: Phòng 101 có 2 giường Single
}