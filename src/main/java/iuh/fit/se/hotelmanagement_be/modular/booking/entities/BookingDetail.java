package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "booking_details")
public class BookingDetail {
    @Id
    @Column(name = "booking_detail_id")
    String id;

    LocalDateTime checkinTime;
    LocalDateTime checkoutTime;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    Booking booking;


    @OneToMany(mappedBy = "bookingDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    List<BookingServiceDetail> bookingServiceDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    Room room;


    Integer numAdults;


    Integer numChildren;


    Integer numInfants;

    Double baseRoomPricePerNight;       // Giá phòng gốc mỗi đêm (chưa phụ thu)
    Double extraAdultFeePerNight;       // Tiền phụ thu người lớn mỗi đêm
    Double extraChildFeePerNight;       // Tiền phụ thu trẻ em mỗi đêm
    Double roomSubTotal;                // Tổng tiền phòng (đã nhân số đêm + phụ thu)
    Double serviceSubTotal;             // Tổng tiền dịch vụ của phòng này
    Double totalPrice;                  // Tổng cộng cuối cùng của chi tiết này (Phòng + Dịch vụ)

    // Trang  thai cua bookingdetail
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    BookingStatusType status; // Ví dụ: PENDING, CHECKED_IN, CHECKED_OUT, CANCELLED

    LocalDateTime actualCheckInTime;  // Thời gian khách thực tế nhận phòng
    LocalDateTime actualCheckOutTime; // Thời gian khách thực tế trả phòng

    BigDecimal earlyCheckInFee = BigDecimal.ZERO;  // Phí check-in sớm
    BigDecimal lateCheckOutFee = BigDecimal.ZERO; // Phí check-out muộn
    BigDecimal otherSurcharges = BigDecimal.ZERO;  // Các phụ thu khác (nếu có)

    //
    // MỚI THÊM: Lưu lại chính xác thời điểm phòng này bị bấm hủy
    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    // MỚI THÊM: Ai là người thực hiện hủy phòng này (Nhân viên nào)
    @Column(name = "cancelled_by")
    String cancelledBy;

    // --- TỰ ĐỘNG SINH MÃ CHI TIẾT ĐẶT PHÒNG TRƯỚC KHI LƯU ---
    @PrePersist
    protected void onCreate() {
        if (this.id == null || this.id.isEmpty()) {
            String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDateTime.now());
            int randomNum = (int) (Math.random() * 900000) + 100000;
            this.id = "BD" + dateStr + randomNum;
        }
    }
}
