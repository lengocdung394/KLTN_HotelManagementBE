package iuh.fit.se.hotelmanagement_be.modular.promotion.repositories;

import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.SavedPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPromotionRepository extends JpaRepository<SavedPromotion, Long> {

    List<SavedPromotion> findAllByAccountIdOrderBySavedAtDesc(Long accountId);

    Optional<SavedPromotion> findByAccountIdAndPromotionId(Long accountId, Long promotionId);

    boolean existsByAccountIdAndPromotionId(Long accountId, Long promotionId);

    void deleteByAccountIdAndPromotionId(Long accountId, Long promotionId);
}