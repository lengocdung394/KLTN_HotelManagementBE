package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.PaymentTransaction;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
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
}
