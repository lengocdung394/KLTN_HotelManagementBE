package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "booking_service_usages",
        indexes = @Index(name = "idx_booking_service_usages_confirmed_at", columnList = "confirmed_at")
)
public class BookingServiceUsage {
    @Id
    @EqualsAndHashCode.Include
    @Column(name = "booking_service_usage_id", length = 36)
    String id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_service_id", nullable = false)
    BookingServiceDetail bookingServiceDetail;

    @Column(nullable = false)
    int quantity;

    @Column(name = "unit_price", nullable = false)
    Double unitPrice;

    @Column(name = "confirmed_at", nullable = false)
    LocalDateTime confirmedAt;

    @Column(name = "confirmed_by")
    String confirmedBy;

    @PrePersist
    void assignId() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }
}
