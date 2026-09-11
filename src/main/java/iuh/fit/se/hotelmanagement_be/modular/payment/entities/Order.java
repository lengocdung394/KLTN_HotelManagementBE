package iuh.fit.se.hotelmanagement_be.modular.payment.entities;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
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


    @OneToOne(mappedBy = "order")
    Booking booking;


    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PaymentTransaction> paymentTransactions;

    public BigDecimal getTotalAmount() {
        BigDecimal roomTotal = roomTotalAmount != null ? roomTotalAmount : BigDecimal.ZERO;
        BigDecimal serviceTotal = serviceTotalAmount != null ? serviceTotalAmount : BigDecimal.ZERO;
        BigDecimal discRoom = discountRoomAmount != null ? discountRoomAmount : BigDecimal.ZERO;
        BigDecimal discService = discountServiceAmount != null ? discountServiceAmount : BigDecimal.ZERO;
        BigDecimal discTotal = discountAmountTotal != null ? discountAmountTotal : BigDecimal.ZERO;

        BigDecimal finalRoom = roomTotal.subtract(discRoom);
        BigDecimal finalService = serviceTotal.subtract(discService);
        BigDecimal subTotal = finalRoom.add(finalService);

        // Đảm bảo tổng tiền không bị âm
        return subTotal.subtract(discTotal).max(BigDecimal.ZERO);
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
}
