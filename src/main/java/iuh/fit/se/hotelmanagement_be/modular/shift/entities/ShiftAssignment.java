package iuh.fit.se.hotelmanagement_be.modular.shift.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "shift_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftAssignment {

    @Id
    @Column(name = "shift_assignment_id", length = 50)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @Column(nullable = false)
    LocalDate workDate;

    // "Ca sáng" hoặc "Ca tối" hoặc "Ca đêm"
    @Column(nullable = false, length = 50)
    String shiftType;

    // "06:00 – 14:00" hoặc "14:00 – 22:00"
    String shiftTime;

    // "Lễ tân" hoặc "Housekeeping"
    @Column(nullable = false, length = 100)
    String role;

    // "Trực quầy lễ tân", "Dọn phòng theo tầng", etc.
    String task;

    // "SCHEDULED", "COMPLETED", "ABSENT", "CANCELLED"
    @Builder.Default
    String status = "SCHEDULED";

    @Column(length = 500)
    String note;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null || this.id.isBlank()) {
            String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
            int rand = (int) (Math.random() * 9000) + 1000;
            this.id = "SHF" + dateStr + rand;
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "SCHEDULED";
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
