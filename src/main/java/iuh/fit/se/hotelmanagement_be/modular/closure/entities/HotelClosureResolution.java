package iuh.fit.se.hotelmanagement_be.modular.closure.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "hotel_closure_resolutions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"closure_schedule_id", "booking_id"}))
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelClosureResolution {
    @Id
    @Column(name = "closure_resolution_id", length = 50)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closure_schedule_id", nullable = false)
    HotelClosureSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    Booking booking;

    @Column(nullable = false, length = 40)
    String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_account_id", nullable = false)
    Account resolvedBy;

    @Column(name = "resolved_at", nullable = false)
    LocalDateTime resolvedAt;

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) {
            id = "CLR" + java.util.UUID.randomUUID().toString().replace("-", "");
        }
        resolvedAt = LocalDateTime.now();
    }
}
