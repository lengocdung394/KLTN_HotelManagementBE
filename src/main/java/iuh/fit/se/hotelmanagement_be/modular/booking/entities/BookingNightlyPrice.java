package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "booking_detail_nightly_prices",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_detail_nightly_price_date",
                columnNames = {"booking_detail_id", "stay_date"}
        )
)
public class BookingNightlyPrice {
    @Id
    @Column(name = "booking_nightly_price_id", length = 36)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_detail_id", nullable = false)
    BookingDetail bookingDetail;

    @Column(name = "stay_date", nullable = false)
    LocalDate stayDate;

    @Column(name = "room_base_price", precision = 19, scale = 2)
    BigDecimal roomBasePrice;

    @Column(name = "extra_adult_fee", precision = 19, scale = 2)
    BigDecimal extraAdultFee;

    @Column(name = "extra_child_fee", precision = 19, scale = 2)
    BigDecimal extraChildFee;

    @Column(name = "extra_adult_unit_price", precision = 19, scale = 2)
    BigDecimal extraAdultUnitPrice;

    @Column(name = "extra_child_unit_price", precision = 19, scale = 2)
    BigDecimal extraChildUnitPrice;

    @Column(name = "standard_capacity")
    Integer standardCapacity;

    @Column(name = "max_extra_guests")
    Integer maxExtraGuests;

    @Column(name = "night_total", nullable = false, precision = 19, scale = 2)
    BigDecimal nightTotal;

    @PrePersist
    void assignId() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    }
}
