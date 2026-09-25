package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.booking.id = :bookingId")
    Optional<Order> findByBookingId(@Param("bookingId") Long bookingId);

    List<Order> findByOrderStatus(OrderStatusType orderStatus);

    @Query("SELECT o FROM Order o WHERE o.issueDate BETWEEN :startDate AND :endDate")
    List<Order> findByIssueDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
