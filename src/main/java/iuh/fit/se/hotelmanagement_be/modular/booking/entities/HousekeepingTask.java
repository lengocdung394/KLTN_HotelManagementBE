package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "housekeeping_tasks", indexes = {
        @Index(name = "idx_housekeeping_tasks_assignee_status", columnList = "assigned_employee_id, status"),
        @Index(name = "idx_housekeeping_tasks_status_assigned", columnList = "status, assigned_at")
})
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HousekeepingTask {
    @Id
    @Column(length = 36)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    Room room;

    @Column(name = "hotel_id", nullable = false)
    Long hotelId;

    @Column(name = "booking_detail_id", nullable = false, length = 50)
    String bookingDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    Employee assignedEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_employee_id")
    Employee backupEmployee;

    @Column(nullable = false, length = 30)
    String status;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "assigned_at")
    LocalDateTime assignedAt;

    @Column(name = "reassigned_at")
    LocalDateTime reassignedAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
