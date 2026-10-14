package iuh.fit.se.hotelmanagement_be.modular.promotion.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import java.util.UUID; // Thêm thư viện UUID

@Entity
@Table(
        name = "user_promotion_usage",
        // Prevents concurrent bookings from reserving the same promotion for one customer.
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"customer_id", "promotion_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPromotionUsage {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id; // Đổi kiểu dữ liệu từ Long sang String

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Store the stable campaign code; customer-specific vouchers use their parent promotion code.
    @Column(name = "promotion_code", nullable = false, length = 50)
    private String promotionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt; // Time the code was applied; the row is removed if the booking drops it.

    // Tự động sinh ID dạng UUID dạng String trước khi lưu vào Database (nếu chưa có ID)
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}