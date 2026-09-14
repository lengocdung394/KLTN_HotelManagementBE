package iuh.fit.se.hotelmanagement_be.modular.room.entities;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "room_seasonal_rates")
public class RoomSeasonalRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull(message = "Chi nhánh không được để trống")
    @Column(name = "hotel_id", nullable = false)
    Long hotelId;

    @NotNull(message = "Loại phòng không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    RoomType roomType;

    @NotNull(message = "Tên đợt giá không được để trống")
    @Column(name = "rate_name", nullable = false)
    String rateName; // Tên đợt giá (VD: "Lễ Quốc Khánh 2/9", "Mùa cao điểm Hè 2026")

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Column(name = "start_date", nullable = false)
    LocalDate startDate; // Ngày bắt đầu khung giá

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Column(name = "end_date", nullable = false)
    LocalDate endDate; // Ngày kết thúc khung giá

    @NotNull(message = "Giá phòng theo khung thời gian không được để trống")
    @DecimalMin(value = "0.0", message = "Giá phòng không được âm")
    @Column(name = "price", nullable = false)
    Double price; // Mức giá áp dụng trong khung này
}
