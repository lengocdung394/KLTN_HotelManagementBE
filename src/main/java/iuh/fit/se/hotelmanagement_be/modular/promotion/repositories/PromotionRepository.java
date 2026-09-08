package iuh.fit.se.hotelmanagement_be.modular.promotion.repositories;

import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByIdAndDeletedFalse(Long id);

    boolean existsByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndIdNotAndDeletedFalse(String code, Long id);

    @Query("""
            SELECT p FROM Promotion p
            WHERE p.deleted = false
              AND (:status IS NULL OR p.status = :status)
              AND (:type IS NULL OR p.type = :type)
              AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                   OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:startDate IS NULL OR p.startDate >= :startDate)
              AND (:endDate IS NULL OR p.endDate <= :endDate)
            ORDER BY p.createdAt DESC
            """)
    Page<Promotion> findAllWithFilters(
            @Param("status") PromotionStatus status,
            @Param("type") PromotionType type,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    List<Promotion> findAllByStatusAndDeletedFalseOrderByCreatedAtDesc(PromotionStatus status);

    @Modifying
    @Query("""
            UPDATE Promotion p
            SET p.status = 'EXPIRED', p.updatedAt = :now
            WHERE p.deleted = false
              AND p.status = 'ACTIVE'
              AND p.endDate < :now
            """)
    int bulkExpirePromotions(@Param("now") LocalDateTime now);
}
