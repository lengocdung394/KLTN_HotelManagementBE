package iuh.fit.se.hotelmanagement_be.modular.promotion.repositories;

import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.UserPromotionUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserPromotionUsageRepository extends JpaRepository<UserPromotionUsage, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from UserPromotionUsage u
            where u.customer.id = :customerId
              and u.promotionCode = :promotionCode
            """)
    Optional<UserPromotionUsage> findByCustomerAndPromotionCodeForUpdate(
            @Param("customerId") String customerId,
            @Param("promotionCode") String promotionCode
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from UserPromotionUsage u
            where u.customer.id = :customerId
              and u.booking.id = :bookingId
            """)
    Optional<UserPromotionUsage> findByCustomerAndBookingForUpdate(
            @Param("customerId") String customerId,
            @Param("bookingId") String bookingId
    );
}
