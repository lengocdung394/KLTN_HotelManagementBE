package iuh.fit.se.hotelmanagement_be.modular.promotion.repositories;

import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerPromotionRepository extends JpaRepository<CustomerPromotion, Long> {

    boolean existsByCustomerIdAndPromotionId(Long customerId, Long promotionId);
    Optional<CustomerPromotion> findByUniqueCodeAndCustomerId(String uniqueCode, Long customerId);
}
