package iuh.fit.se.hotelmanagement_be.modular.promotion.repositories;

import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPromotionRepository extends JpaRepository<CustomerPromotion, String> {

    boolean existsByCustomerIdAndPromotionId(String customerId, String promotionId);

    Optional<CustomerPromotion> findByUniqueCodeAndCustomerId(String uniqueCode, String customerId);

    List<CustomerPromotion> findByCustomerId(String customerId);

    Optional<CustomerPromotion> findById(String id);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cp from CustomerPromotion cp
            where cp.id = :id
              and cp.customer.id = :customerId
            """)
    Optional<CustomerPromotion> findByIdAndCustomerIdForUpdate(
            @Param("id") String id,
            @Param("customerId") String customerId
    );

    //Khóa voucher khi áp dụng để hai request không giữ cùng một mã cho nhiều booking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select cp from CustomerPromotion cp
            where cp.id = :id
              and cp.customer.id = :customerId
            """)
    Optional<CustomerPromotion> findForUpdate(
            @Param("id") String id,
            @Param("customerId") String customerId
    );

    // Tìm voucher đang giữ cho booking, dùng lúc đổi mã, hủy booking hoặc check-in
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select cp from CustomerPromotion cp
    where cp.booking.id = :bookingId
      and cp.isUsed = false
    """)
    Optional<CustomerPromotion> findReservedByBookingForUpdate(
            @Param("bookingId") String bookingId
    );
}
