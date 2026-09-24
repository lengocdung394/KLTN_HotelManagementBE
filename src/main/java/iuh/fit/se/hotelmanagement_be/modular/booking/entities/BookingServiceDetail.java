package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import iuh.fit.se.hotelmanagement_be.modular.service.entities.Service;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "booking_services")
public class BookingServiceDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_service_id")
    Long id;

    int quantity;

    String name;
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_detail_id", nullable = false)
    BookingDetail bookingDetail;

    LocalDateTime usedAt;

    Double price;

    // Khóa ngoại trỏ về Danh mục Service
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    Service service;


    @Builder.Default
    @Column(name = "is_paid", nullable = false)
    Boolean isPaid = false; // Mặc định dịch vụ mới thêm là chưa thanh toán

    @Column(name = "paid_at")
    LocalDateTime paidAt;   // Thời điểm dịch vụ này được thanh toán (lúc checkout)


    // MỚI THÊM:
    @Builder.Default
    @Column(name = "cancelled", nullable = false)
    Boolean cancelled = false;   // dịch vụ đã thanh toán nhưng bị hủy sau đó -> true

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    @PrePersist
    protected void onCreate() {
        if (this.usedAt == null) {
            this.usedAt = LocalDateTime.now();
        }
        if (this.isPaid == null) {
            this.isPaid = false;
        }
        if (this.cancelled == null) {
            this.cancelled = false;
        }
    }
}
