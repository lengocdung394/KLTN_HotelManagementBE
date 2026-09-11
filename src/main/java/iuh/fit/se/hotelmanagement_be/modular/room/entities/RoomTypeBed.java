package iuh.fit.se.hotelmanagement_be.modular.room.entities;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
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
        name = "room_type_beds",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"room_type", "bed_type_id"})
        }
)
public class RoomTypeBed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull(message = "Loại phòng không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    RoomType roomType; // Map trực tiếp sang Enum RoomType giống như ở BranchRoomPolicy của bạn

    @NotNull(message = "Loại giường không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bed_type_id", nullable = false)
    BedType bedType;

    @NotNull(message = "Số lượng giường không được để trống")
    @Min(value = 1, message = "Số lượng giường tối thiểu phải là 1")
    @Column(name = "quantity", nullable = false)
    Integer quantity; // Ví dụ: 2 (cho phòng FAMILY có 2 giường Queen)
}