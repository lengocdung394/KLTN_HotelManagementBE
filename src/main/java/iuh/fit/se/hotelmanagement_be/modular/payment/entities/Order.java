package iuh.fit.se.hotelmanagement_be.modular.payment.entities;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentStatus;
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
    @Column(name = "order_id")
    String id;

    LocalDateTime issueDate;

    LocalDateTime closeDate;

    BigDecimal roomTotalAmount; // tong tien phong

    BigDecimal serviceTotalAmount; // tong tien dich vu

    BigDecimal discountRoomAmount; // giam tien phong

    BigDecimal discountServiceAmount; // giam tien dịch vu

    BigDecimal discountAmountTotal; // giam tien tat ca (dich vu + tien phong)

    BigDecimal paidAmount; // tien da thanh toán

    BigDecimal remainingAmount;

    OrderStatusType orderStatus;
    PaymentStatus  paymentStatus;

    @ElementCollection
    @CollectionTable(name = "order_payment_codes", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "payment_order_code")
    List<Long> paymentOrderCodes;

    @OneToOne(mappedBy = "order")
    Booking booking;

    //MỚI THÊM: Thêm field này để Hibernate tạo cột dưới Database
    BigDecimal totalAmount;

    BigDecimal surchargeTotalAmount; // Tổng tiền phụ thu (check-in sớm, check-out muộn, làm hỏng đồ...)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PaymentTransaction> paymentTransactions;

    // CHỈNH SỬA: Sửa lại hàm get để trả về giá trị của field hoặc tính toán nếu field trống
    public BigDecimal getTotalAmount() {
        if (this.totalAmount != null && this.totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return this.totalAmount;
        }
        return calculateActualTotal();
    }

    // Hàm nội bộ dùng để tính toán tổng tiền từ các khoản thành phần
    private BigDecimal calculateActualTotal() {
        BigDecimal roomTotal = roomTotalAmount != null ? roomTotalAmount : BigDecimal.ZERO;
        BigDecimal serviceTotal = serviceTotalAmount != null ? serviceTotalAmount : BigDecimal.ZERO;
        BigDecimal surchargeTotal = surchargeTotalAmount != null ? surchargeTotalAmount : BigDecimal.ZERO;
        BigDecimal discRoom = discountRoomAmount != null ? discountRoomAmount : BigDecimal.ZERO;
        BigDecimal discService = discountServiceAmount != null ? discountServiceAmount : BigDecimal.ZERO;
        BigDecimal discTotal = discountAmountTotal != null ? discountAmountTotal : BigDecimal.ZERO;

        BigDecimal finalRoom = roomTotal.subtract(discRoom);
        BigDecimal finalService = serviceTotal.subtract(discService);
        BigDecimal subTotal = finalRoom.add(finalService).add(surchargeTotal);

        return subTotal.subtract(discTotal).max(BigDecimal.ZERO);
    }


    public BigDecimal getRemainingAmount() {
        BigDecimal total = getTotalAmount(); // Hàm này giờ sẽ lấy từ field totalAmount ra rất nhanh
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

    // --- TỰ ĐỘNG SINH MÃ ORDER TRƯỚC KHI LƯU ---
    @PrePersist
    protected void onCreate() {
        if (this.issueDate == null) {
            this.issueDate = LocalDateTime.now();
        }
        if (this.orderStatus == null) {
            this.orderStatus = OrderStatusType.OPEN;
        }
        if (this.id == null || this.id.isEmpty()) {
            String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
            int randomNum = (int) (Math.random() * 9000) + 1000;
            this.id = "ORD" + dateStr + randomNum; // Ví dụ: ORD202609228492
        }
    }
}
