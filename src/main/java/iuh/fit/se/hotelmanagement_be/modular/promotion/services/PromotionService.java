package iuh.fit.se.hotelmanagement_be.modular.promotion.services;

import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionType;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.ChangeStatusRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.CreatePromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.UpdatePromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PageResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PromotionResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionService {

    PromotionResponse createPromotion(CreatePromotionRequest request);

    PromotionResponse getPromotionById(Long id);

    PageResponse<PromotionResponse> getAllPromotions(
            PromotionStatus status,
            PromotionType type,
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    List<PromotionResponse> getActivePromotions();

    PromotionResponse updatePromotion(Long id, UpdatePromotionRequest request);

    PromotionResponse changeStatus(Long id, ChangeStatusRequest request);

    void deletePromotion(Long id);
}
