package iuh.fit.se.hotelmanagement_be.modular.payment.repositories;

import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 💡 SỬA TẠI ĐÂY: Chuyển hàm existsBy sang Query thuần để quét trong danh sách Collection
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.paymentOrderCodes c WHERE c = :paymentOrderCode")
    boolean existsByPaymentOrderCode(@Param("paymentOrderCode") Long paymentOrderCode);

    // Hàm tìm kiếm theo mã code đã viết chuẩn bằng Query thuần
    @Query("SELECT o FROM Order o JOIN o.paymentOrderCodes c WHERE c = :paymentOrderCode")
    Optional<Order> findByPaymentOrderCodesContaining(@Param("paymentOrderCode") Long paymentOrderCode);
}
