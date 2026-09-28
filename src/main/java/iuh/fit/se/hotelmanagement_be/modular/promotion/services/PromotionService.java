package iuh.fit.se.hotelmanagement_be.modular.promotion.services;

import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionScope;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.ChangeStatusRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.CreatePromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.UpdatePromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PageResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PromotionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionService {

    PromotionResponse createPromotion(CreatePromotionRequest request, MultipartFile imageFile);

    PromotionResponse getPromotionById(String id);

    PageResponse<PromotionResponse> getAllPromotions(
            Long hotelId,
            PromotionStatus status,
            PromotionScope type,
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    List<PromotionResponse> getActivePromotions();

    PromotionResponse updatePromotion(String id, UpdatePromotionRequest request, MultipartFile imageFile);

    PromotionResponse changeStatus(String id, ChangeStatusRequest request);

    void deletePromotion(String id);
}
