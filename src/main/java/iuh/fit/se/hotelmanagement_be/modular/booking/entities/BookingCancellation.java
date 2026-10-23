package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationRefundMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "booking_cancellations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_booking_cancellation_booking", columnNames = "booking_id")
})
public class BookingCancellation {
    @Id
    @Column(name = "cancellation_id", nullable = false, updatable = false)
    String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    BookingCancellationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    BookingCancellationRefundMethod refundMethod;

    @Column(length = 500)
    String reason;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal refundAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal roomRefundAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal serviceRefundAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal roomPenaltyAmount;

    @Column(nullable = false)
    long hoursBeforeCheckIn;

    @Column(nullable = false, length = 50)
    String refundPolicy;

    @Column(length = 120)
    String bankName;

    @Column(length = 30)
    String bankCode;

    @Column(length = 120)
    String bankAccountName;

    @Column(length = 50)
    String bankAccountNumber;

    @Column(length = 100)
    String requestedByCustomerId;
    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "request_by_account_id") // tai khoan nhan vien huy booking cho khach hang
    Account requestedByAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_account_id")
    Account reviewedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refunded_by_account_id")
    Account refundedBy;

    @Column(length = 500)
    String reviewNote;

    @Column(length = 120)
    String refundReference;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    LocalDateTime requestedAt;

    @Column(name = "refund_email", length = 254)
    String refundEmail;

    LocalDateTime reviewedAt;

    LocalDateTime refundedAt;

    LocalDateTime cancelledAt;
}
