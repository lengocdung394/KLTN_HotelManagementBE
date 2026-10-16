package iuh.fit.se.hotelmanagement_be.modular.payment.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "checkout_refunds", uniqueConstraints = {
        @UniqueConstraint(name = "uk_checkout_refund_booking", columnNames = "booking_id")
})
public class CheckoutRefund {
    @Id
    @Column(name = "checkout_refund_id", nullable = false, updatable = false, length = 40)
    String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    Booking booking;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    PaymentType refundMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    CheckoutRefundStatus status;

    @Column(length = 120)
    String bankName;

    @Column(length = 30)
    String bankCode;

    @Column(length = 120)
    String bankAccountName;

    @Column(length = 50)
    String bankAccountNumber;

    @Column(length = 500)
    String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_account_id")
    Account approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refunded_by_account_id")
    Account refundedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    LocalDateTime requestedAt;

    LocalDateTime approvedAt;
    LocalDateTime refundedAt;
}
