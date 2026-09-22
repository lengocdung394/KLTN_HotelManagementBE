package iuh.fit.se.hotelmanagement_be.modular.promotion.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.CustomerPromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.ClaimPromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.CustomerPromotionResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PromotionGetListByCustomerResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.services.CustomerPromotionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class CustomerPromotionServiceImpl implements CustomerPromotionService {
     PromotionRepository promotionRepository;
     CustomerPromotionRepository customerPromotionRepository;
     // Đảm bảo class của bạn đã có annotation này để sử dụng biến `log`
    @Override
    public List<PromotionGetListByCustomerResponse> getPromotionsByCustomerId(String customerId) {
        log.info("==> [API] Đang lấy danh sách mã khuyến mãi cho khách hàng có ID: {}...", customerId);

        List<CustomerPromotion> customerPromotions = customerPromotionRepository.findByCustomerId(customerId);
        log.info("==> [SUCCESS] Tìm thấy tổng cộng {} mã khuyến mãi cho khách hàng [ID: {}].", customerPromotions.size(), customerId);

        return customerPromotions.stream().map(cp -> {
                    Promotion p = cp.getPromotion();

                    // Kiểm tra an toàn tránh lỗi NullPointerException nếu Promotion bị null
                    if (p == null) {
                        log.warn("Cảnh báo: CustomerPromotion [ID: {}] có liên kết đến Promotion bị null!", cp.getId());
                        return null;
                    }

                    log.debug("Mapping khuyến mãi [ID: {}, Code riêng biệt: {}, Tên: {}] cho khách hàng ID: {}",
                            p.getId(), cp.getUniqueCode(), p.getName(), customerId);

                    return PromotionGetListByCustomerResponse.builder()
                            .id(cp.getId())
                            .code(cp.getUniqueCode()) // mã riêng biệt nè
                            .name(p.getName())
                            .description(p.getDescription()) // Đảm bảo truyền mô tả ở đây
                            .type(p.getType().name())
                            .discountValue(p.getDiscountValue())
                            .maxDiscountAmount(p.getMaxDiscountAmount())
                            .minBookingValue(p.getMinBookingValue())
                            .startDate(p.getStartDate())
                            .endDate(p.getEndDate())
                            .exclusive(p.isExclusive())
                            .build();
                })
                .filter(response -> response != null) // Lọc bỏ các phần tử null nếu có cảnh báo trên
                .toList();
    }

    @Override
    @Transactional
    public CustomerPromotionResponse claimPromotion(ClaimPromotionRequest request) {

        //1. Lay thong tin customer dang nhap
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // 2. Lay customerId tu UserDetails
        Account account = (Account) authentication.getPrincipal();
        Customer customer = account.getCustomer();
        if (customer == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. Tìm Promotion & Validate điều kiện
        Promotion promotion = promotionRepository.findByIdAndDeletedFalse(request.getPromotionId())
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new AppException(ErrorCode.PROMOTION_EXPIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new AppException(ErrorCode.PROMOTION_EXPIRED);
        }

        if (promotion.getUsageLimit() != null && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new AppException(ErrorCode.PROMOTION_USAGE_LIMIT_EXCEEDED);
        }

        // 3. Kiểm tra xem khách hàng đã lưu mã này chưa
        boolean alreadyClaimed = customerPromotionRepository.existsByCustomerIdAndPromotionId(
                customer.getId(), promotion.getId());
        if (alreadyClaimed) {
            throw new IllegalArgumentException("Bạn đã lưu mã khuyến mãi này trước đó rồi!");
        }
        // Khong can phai get ma , vi da co cai @PrePersist
        //4. Tao bang ghi CustomerPromotion
        CustomerPromotion customerPromotion = CustomerPromotion.builder()
                .customer(customer)
                .promotion(promotion)
                .isUsed(false)
                .createdAt(now).build();
        customerPromotionRepository.save(customerPromotion);
        log.info("Khách hàng ID {} đã lưu thành công khuyến mãi ID {}", customer.getId(), promotion.getId());

        return toCustomerPromotionResponse(customerPromotion);
    }


    private CustomerPromotionResponse toCustomerPromotionResponse(CustomerPromotion cp) {
        Promotion promotion = cp.getPromotion();

        return CustomerPromotionResponse.builder()
                .id(cp.getId())
                .voucherCode(cp.getUniqueCode())
                .isUsed(cp.isUsed())
                .savedAt(cp.getCreatedAt())
                .usedAt(cp.getUsedAt())
                // Information from Promotion
                .promotionId(promotion.getId())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .type(promotion.getType())
                .discountValue(promotion.getDiscountValue())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .minBookingValue(promotion.getMinBookingValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .hotelId(promotion.getHotel() != null ? promotion.getHotel().getId() : null)
                .hotelName(promotion.getHotel() != null ? promotion.getHotel().getName() : "Toàn hệ thống")
                .build();
    }

}
