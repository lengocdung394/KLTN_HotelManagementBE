package iuh.fit.se.hotelmanagement_be.modular.promotion.repositories;

import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface CustomerPromotionRepository extends JpaRepository<CustomerPromotion, Long> {

    boolean existsByCustomerIdAndPromotionId(String customerId, String promotionId);
    Optional<CustomerPromotion> findByUniqueCodeAndCustomerId(String uniqueCode, String customerId);
    List<CustomerPromotion> findByCustomerId(String customerId);
}
