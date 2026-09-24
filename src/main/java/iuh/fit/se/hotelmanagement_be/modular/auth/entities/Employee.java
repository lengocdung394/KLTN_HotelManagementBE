package iuh.fit.se.hotelmanagement_be.modular.auth.entities;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@EqualsAndHashCode()
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @Column(name = "employee_id")
    String id;

    String cccd;

    String fullName;

    String address;

    String position;

    String phone;

    String avatarUrl;
    LocalDate dateOfBirth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    Hotel hotel;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    Account account;

    // --- TỰ ĐỘNG SINH MÃ NHÂN VIÊN TRƯỚC KHI LƯU ---
    @PrePersist
    protected void onCreate() {
        if (this.id == null || this.id.isEmpty()) {
            String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
            int randomNum = (int) (Math.random() * 9000) + 1000; // Số ngẫu nhiên 4 chữ số
            this.id = "EMP" + dateStr + randomNum; // Ví dụ: EMP202609228492
        }
    }

}
