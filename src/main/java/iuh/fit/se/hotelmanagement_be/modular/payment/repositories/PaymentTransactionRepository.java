package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.PaymentTransaction;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    interface CashFlowReportProjection {
        BigDecimal getCashReceived();
        BigDecimal getCashRefunded();
        BigDecimal getCashChangeReturned();
    }

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN pt.cashFlowType = :receiptType THEN pt.amount END), 0)
                       AS cashReceived,
                   COALESCE(SUM(CASE WHEN pt.cashFlowType = :refundType THEN pt.amount END), 0)
                       AS cashRefunded,
                   COALESCE(SUM(CASE WHEN pt.cashFlowType = :changeType THEN pt.amount END), 0)
                       AS cashChangeReturned
            FROM PaymentTransaction pt
            WHERE pt.order.booking.hotel.id = :hotelId
              AND pt.transactionDate >= :start
              AND pt.transactionDate < :end
              AND pt.cashFlowType IN (:receiptType, :refundType, :changeType)
            """)
    CashFlowReportProjection getCashFlowReport(
            @Param("hotelId") Long hotelId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("receiptType") CashFlowType receiptType,
            @Param("refundType") CashFlowType refundType,
            @Param("changeType") CashFlowType changeType
    );

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.order.id = :orderId ORDER BY pt.id DESC")
    List<PaymentTransaction> findByOrderId(@Param("orderId") String orderId);
}
