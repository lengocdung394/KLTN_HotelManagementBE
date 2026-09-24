package iuh.fit.se.hotelmanagement_be.modular.promotion.services;

import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.ClaimPromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.CustomerPromotionResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PromotionGetListByCustomerResponse;

import java.util.List;

public interface CustomerPromotionService {
    List<PromotionGetListByCustomerResponse> getPromotionsByCustomerId(String customerId);
    CustomerPromotionResponse claimPromotion(ClaimPromotionRequest request);
}
