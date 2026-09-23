package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
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
    @Column(name = "booking_id")
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    Customer customer;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    Employee employee;

    BookingStatus bookingStatus;

    BookingChannel bookingChannel;

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
    // Thêm liên kết tới Chi nhánh/Khách sạn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel; // Hoặc Branch branch; tùy theo tên entity của bạn

    //
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id")
    private Order order;

    // ma booking thay doi
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.bookingStatus == null) {
            this.bookingStatus = BookingStatus.PENDING;
        }

        // Tự động sinh mã ID dạng String ngay trước khi insert vào database
        if (this.id == null || this.id.isEmpty()) {
            String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
            int randomNum = (int) (Math.random() * 9000) + 1000; // 4 số ngẫu nhiên để tránh trùng
            this.id = "BK" + dateStr + randomNum; // Ví dụ: BK202609228492
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
