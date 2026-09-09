package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    Customer customer;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    Employee employee;

    BookingStatus bookingStatus;

    BookingChannel bookingChannel;

    LocalDateTime createAt;

    @ToString.Exclude
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    List<BookingDetail> bookingDetails = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_promotion_id")
    CustomerPromotion customerPromotion;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    // 3. Tiền giảm & thời điểm áp dụng (khớp với sơ đồ Class của bạn)
    @Column(name = "apply_amount", precision = 15, scale = 2)
    BigDecimal applyAmount; // Số tiền được giảm (VD: 100.000 VNĐ)

    @Column(name = "apply_at")
    LocalDateTime applyAt;  // Thời điểm nhân viên bấm áp dụng mã

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    Promotion promotion;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.bookingStatus == null) {
            this.bookingStatus = BookingStatus.PENDING;
        }
    }

    public void addBookingDetail(BookingDetail detail) {
        if (this.bookingDetails == null) {
            this.bookingDetails = new ArrayList<>();
        }
        this.bookingDetails.add(detail);
        detail.setBooking(this);
    }
}
