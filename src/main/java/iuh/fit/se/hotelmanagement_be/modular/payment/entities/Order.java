package iuh.fit.se.hotelmanagement_be.modular.payment.entities;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    Long id;

    LocalDateTime issueDate;

    LocalDateTime closeDate;

    BigDecimal roomTotalAmount;

    BigDecimal serviceTotalAmount;

    BigDecimal discountRoomAmount;

    BigDecimal discountServiceAmount;

    BigDecimal discountAmountTotal;

    BigDecimal paidAmount;

    OrderStatusType orderStatus;
    @Column(name = "payment_order_code", unique = true)
    Long paymentOrderCode;

    @OneToOne(mappedBy = "order")
    Booking booking;

    BigDecimal surchargeTotalAmount; // Tổng tiền phụ thu (check-in sớm, check-out muộn, làm hỏng đồ...)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PaymentTransaction> paymentTransactions;

    public BigDecimal getTotalAmount() {
        BigDecimal roomTotal = roomTotalAmount != null ? roomTotalAmount : BigDecimal.ZERO;
        BigDecimal serviceTotal = serviceTotalAmount != null ? serviceTotalAmount : BigDecimal.ZERO;
        BigDecimal surchargeTotal = surchargeTotalAmount != null ? surchargeTotalAmount : BigDecimal.ZERO; // Thêm dòng này
        BigDecimal discRoom = discountRoomAmount != null ? discountRoomAmount : BigDecimal.ZERO;
        BigDecimal discService = discountServiceAmount != null ? discountServiceAmount : BigDecimal.ZERO;
        BigDecimal discTotal = discountAmountTotal != null ? discountAmountTotal : BigDecimal.ZERO;

        BigDecimal finalRoom = roomTotal.subtract(discRoom);
        BigDecimal finalService = serviceTotal.subtract(discService);

        // Gộp thêm phụ thu vào tổng tiền
        BigDecimal subTotal = finalRoom.add(finalService).add(surchargeTotal);

        // Đảm bảo tổng tiền không bị âm
        return subTotal.subtract(discTotal).max(BigDecimal.ZERO);
        // Đảm bảo tổng tiền không bị âm
    }

    public BigDecimal getRemainingAmount() {
        BigDecimal total = getTotalAmount();
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;

        return total.subtract(paid).max(BigDecimal.ZERO);
    }

    public void operation() {
        if (getRemainingAmount().compareTo(BigDecimal.ZERO) == 0 && this.orderStatus == OrderStatusType.OPEN) {
            this.orderStatus = OrderStatusType.CLOSED;
            this.closeDate = LocalDateTime.now();
        }
    }

    public void addPaymentSuccess(BigDecimal amountPaid, PaymentType paymentType, String paymentLinkId) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .amount(amountPaid)
                .transactionDate(LocalDateTime.now())
                .paymentType(paymentType)
                .cashFlowType(CashFlowType.RECEIPT)
                .note("PayOS Webhook Ref: " + paymentLinkId)
                .order(this)
                .build();

        if (this.paymentTransactions == null) {
            this.paymentTransactions = new java.util.ArrayList<>();
        }
        this.paymentTransactions.add(transaction);

        BigDecimal currentPaid = this.paidAmount != null ? this.paidAmount : BigDecimal.ZERO;
        this.setPaidAmount(currentPaid.add(amountPaid));

        this.operation(); // Kích hoạt logic đóng phòng/đóng đơn của bạn
    }
}
