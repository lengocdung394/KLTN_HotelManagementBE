package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByPaymentOrderCode(Long paymentOrderCode);
    Optional<Order> findByPaymentOrderCode(Long paymentOrderCode);
}
