package iuh.fit.se.hotelmanagement_be.modular.branch.entities;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
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
        name = "branch_room_policies",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"hotel_id", "room_type"})
        }
)
public class BranchRoomPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull(message = "Chi nhánh khách sạn không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @NotNull(message = "Loại phòng không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    RoomType roomType;

    // --- RÀNG BUỘC PHÍ PHỤ THU ---
    @NotNull(message = "Phí phụ thu người lớn không được để trống")
    @DecimalMin(value = "0.0", message = "Phí phụ thu người lớn không được âm")
    @Column(name = "extra_adult_fee", nullable = false)
    Double extraAdultFee;

    @NotNull(message = "Phí phụ thu trẻ em không được để trống")
    @DecimalMin(value = "0.0", message = "Phí phụ thu trẻ em không được âm")
    @Column(name = "extra_child_fee", nullable = false)
    Double extraChildFee;

    // --- RÀNG BUỘC SỨC CHỨA ---
    @NotNull(message = "Số người lớn tiêu chuẩn không được để trống")
    @Min(value = 1, message = "Số người lớn tiêu chuẩn tối thiểu là 1")
    @Column(name = "standard_adults", nullable = false)
    Integer standardAdults;

    @NotNull(message = "Sức chứa người lớn tối đa không được để trống")
    @Min(value = 1, message = "Sức chứa người lớn tối đa phải ít nhất là 1")
    @Column(name = "max_adults", nullable = false)
    Integer maxAdults;

    @NotNull(message = "Số trẻ em tối đa không được để trống")
    @Min(value = 0, message = "Số trẻ em không được âm")
    @Column(name = "max_children", nullable = false)
    Integer maxChildren;

    @NotNull(message = "Số em bé tối đa không được để trống")
    @Min(value = 0, message = "Số em bé không được âm")
    @Column(name = "max_infants", nullable = false)
    Integer maxInfants;
}