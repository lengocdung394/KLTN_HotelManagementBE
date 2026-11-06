package iuh.fit.se.hotelmanagement_be.modular.closure.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hotel_closure_schedules")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelClosureSchedule {
    @Id
    @Column(name = "closure_schedule_id", length = 50)
    String id;

    @Column(nullable = false, length = 150)
    String title;

    @Column(length = 1000)
    String description;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Column(name = "all_hotels", nullable = false)
    boolean allHotels;

    @ElementCollection
    @CollectionTable(name = "hotel_closure_schedule_hotels",
            joinColumns = @JoinColumn(name = "closure_schedule_id"))
    @Column(name = "hotel_id", nullable = false)
    List<Long> hotelIds = new ArrayList<>();

    @Column(nullable = false)
    boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_account_id", nullable = false)
    Account createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) {
            id = "CLS" + java.util.UUID.randomUUID().toString().replace("-", "");
        }
        createdAt = LocalDateTime.now();
    }
}
