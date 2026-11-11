package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.PaymentTransaction;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, Long> {
    interface OrderRefundTotalProjection {
        String getOrderId();
        BigDecimal getRefundedAmount();
    }

    @Query("""
            SELECT transaction.order.id AS orderId, SUM(transaction.amount) AS refundedAmount
            FROM PaymentTransaction transaction
            WHERE transaction.order.booking.hotel.id = :hotelId
              AND transaction.cashFlowType = :cashFlowType
            GROUP BY transaction.order.id
            """)
    List<OrderRefundTotalProjection> findRefundTotalsByHotelId(
            @Param("hotelId") Long hotelId,
            @Param("cashFlowType") CashFlowType cashFlowType);

    @Query("""
            SELECT transaction.processorEmployee.fullName
            FROM PaymentTransaction transaction
            WHERE transaction.order.id = :orderId
              AND transaction.cashFlowType = :cashFlowType
              AND transaction.paymentType = :paymentType
              AND transaction.amount = :amount
              AND transaction.note = :note
              AND transaction.processorEmployee IS NOT NULL
            ORDER BY transaction.transactionDate DESC
            """)
    List<String> findCheckoutCashRefundEmployeeNames(
            @Param("orderId") String orderId,
            @Param("cashFlowType") CashFlowType cashFlowType,
            @Param("paymentType") PaymentType paymentType,
            @Param("amount") BigDecimal amount,
            @Param("note") String note);
}
