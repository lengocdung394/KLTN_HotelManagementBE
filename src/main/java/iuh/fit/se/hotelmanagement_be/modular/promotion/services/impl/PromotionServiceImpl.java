package iuh.fit.se.hotelmanagement_be.modular.promotion.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionType;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.CustomerPromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.ChangeStatusRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.ClaimPromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.CreatePromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.UpdatePromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.CustomerPromotionResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PageResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PromotionResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.services.PromotionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class PromotionServiceImpl implements PromotionService {

    PromotionRepository promotionRepository;

    // State machine: trạng thái hiện tại → các trạng thái được phép chuyển
    static final Map<PromotionStatus, Set<PromotionStatus>> ALLOWED_TRANSITIONS = Map.of(
            PromotionStatus.DRAFT, Set.of(PromotionStatus.ACTIVE, PromotionStatus.INACTIVE),
            PromotionStatus.ACTIVE, Set.of(PromotionStatus.INACTIVE, PromotionStatus.EXPIRED),
            PromotionStatus.INACTIVE, Set.of(PromotionStatus.ACTIVE, PromotionStatus.EXPIRED),
            PromotionStatus.EXPIRED, Set.of()
    );
    private final HotelRepository hotelRepository;
    private final CustomerPromotionRepository customerPromotionRepository;

    // ==================== CREATE ====================
    @Override
    @Transactional
    public PromotionResponse createPromotion(CreatePromotionRequest request) {
        log.info("Tạo mới khuyến mãi, mã: {}", request.getCode());

        // 1. Kiem tra xac thuc & Lay role tu SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var authorities = authentication.getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean isManager = authorities.stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_MANAGER"));

        if (!isAdmin && !isManager) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Validate Mã duy nhất & Hạn sử dụng
        String code = request.getCode().toUpperCase().trim();
        if (promotionRepository.existsByCodeAndDeletedFalse(code)) {
            throw new AppException(ErrorCode.PROMOTION_CODE_EXISTED);
        }

        validateDates(request.getStartDate(), request.getEndDate());
        validateDiscountValue(request.getType(), request.getDiscountValue());

        // 3. Xử lý gán Khách sạn (Hotel) dựa theo quyền
        Hotel hotel = null;
        Long targetHotelId = request.getHotelId();

// Lấy hotelId của tài khoản đang đăng nhập
        Long accountHotelId = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Account account) {
            accountHotelId = account.getHotelId();
        }

        if (isManager) {
            // Manager BẮT BUỘC dùng hotelId của chính mình
            if (accountHotelId == null) {
                throw new AppException(ErrorCode.MANAGER_HOTEL_NOT_ASSIGNED);
            }
            targetHotelId = accountHotelId;

        } else if (isAdmin) {
            if (targetHotelId == null) {
                // Nếu Admin không truyền hotelId -> Tự động dùng hotelId từ tài khoản Admin
                targetHotelId = accountHotelId;
            } else {
                // Nếu Admin truyền hotelId KHÁC với hotelId của tài khoản mình (khi accountHotelId != null) -> CHẶN
                if (accountHotelId != null && !accountHotelId.equals(targetHotelId)) {
                    throw new AppException(ErrorCode.UNAUTHORIZED); // Hoặc tạo ErrorCode.CANNOT_CREATE_PROMOTION_FOR_OTHER_HOTEL
                }
            }
        }

        // Tìm Hotel từ targetHotelId
        if (targetHotelId != null) {
            hotel = hotelRepository.findById(targetHotelId)
                    .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));
        }
        // 4. Khởi tạo đối tượng Promotion
        Promotion promotion = Promotion.builder()
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .type(request.getType())
                .discountValue(request.getDiscountValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minBookingValue(request.getMinBookingValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .usageLimit(request.getUsageLimit())
                .status(request.getStatus() != null ? request.getStatus() : PromotionStatus.DRAFT)
                .isExclusive(request.isExclusive())
                .hotel(hotel)
                .usedCount(0)
                .deleted(false)
                .build();

        promotion = promotionRepository.save(promotion);
        log.info("Tạo thành công khuyến mãi ID: {} thuộc phạm vi: {}",
                promotion.getId(), hotel != null ? "Chi nhánh ID " + hotel.getId() : "Toàn hệ thống");
        log.info("====== CHECK HOTEL AFTER SAVE ======");
        log.info("Promotion ID: {}", promotion.getId());
        log.info("Hotel Object in Entity: {}", promotion.getHotel());
        log.info("Hotel ID in Entity: {}", promotion.getHotel() != null ? promotion.getHotel().getId() : "NULL (Toàn hệ thống)");
        log.info("====================================");

        log.info("Tạo thành công khuyến mãi ID: {} thuộc phạm vi: {}",
                promotion.getId(), hotel != null ? "Chi nhánh ID " + hotel.getId() : "Toàn hệ thống");
        return toResponse(promotion);
    }

    // ==================== READ ====================
    @Override
    public PromotionResponse getPromotionById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public PageResponse<PromotionResponse> getAllPromotions(
            PromotionStatus status, PromotionType type, String keyword,
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        Page<Promotion> page = promotionRepository.findAllWithFilters(status, type, kw, startDate, endDate, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    public List<PromotionResponse> getActivePromotions() {
        return promotionRepository
                .findAllByStatusAndDeletedFalseOrderByCreatedAtDesc(PromotionStatus.ACTIVE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ==================== UPDATE ====================
    @Override
    @Transactional
    public PromotionResponse updatePromotion(Long id, UpdatePromotionRequest request) {
        log.info("Cập nhật khuyến mãi ID: {}", id);
        Promotion promotion = findOrThrow(id);

        if (promotion.getStatus() == PromotionStatus.ACTIVE && promotion.getUsedCount() > 0) {
            throw new IllegalStateException(
                    "Không thể sửa khuyến mãi đang ACTIVE đã có " + promotion.getUsedCount() + " lượt dùng. Hãy đổi sang INACTIVE trước.");
        }

        validateDates(request.getStartDate(), request.getEndDate());
        validateDiscountValue(request.getType(), request.getDiscountValue());

        promotion.setName(request.getName().trim());
        promotion.setDescription(request.getDescription());
        promotion.setType(request.getType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setMinBookingValue(request.getMinBookingValue());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setUsageLimit(request.getUsageLimit());

        return toResponse(promotionRepository.save(promotion));
    }

    // ==================== CHANGE STATUS ====================
    @Override
    @Transactional
    public PromotionResponse changeStatus(Long id, ChangeStatusRequest request) {
        log.info("Đổi trạng thái khuyến mãi ID: {} → {}", id, request.getStatus());
        Promotion promotion = findOrThrow(id);

        PromotionStatus current = promotion.getStatus();
        PromotionStatus next = request.getStatus();

        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(next)) {
            throw new IllegalStateException("Không thể chuyển trạng thái từ '" + current + "' sang '" + next + "'");
        }

        if (next == PromotionStatus.ACTIVE && promotion.getEndDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Không thể kích hoạt khuyến mãi đã hết hạn");
        }

        promotion.setStatus(next);
        return toResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public CustomerPromotionResponse claimPromotion(ClaimPromotionRequest request) {

        //1. Lay thong tin customer dang nhap
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // â. Lay customerId tu UserDetails
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

        //4. Tao bang ghi CustomerPromotion
        CustomerPromotion customerPromotion = CustomerPromotion.builder()
                .customer(customer)
                .promotion(promotion)
                .isUsed(false)
                .uniqueCode(promotion.getCode())
                .createdAt(now).build();
        customerPromotionRepository.save(customerPromotion);
        log.info("Khách hàng ID {} đã lưu thành công khuyến mãi ID {}", customer.getId(), promotion.getId());

        return toCustomerPromotionResponse(customerPromotion);
    }

    // ==================== DELETE ====================
    @Override
    @Transactional
    public void deletePromotion(Long id) {
        log.info("Xóa mềm khuyến mãi ID: {}", id);
        Promotion promotion = findOrThrow(id);

        if (promotion.getStatus() == PromotionStatus.ACTIVE) {
            throw new IllegalStateException("Không thể xóa khuyến mãi đang ACTIVE. Hãy đổi sang INACTIVE trước.");
        }

        promotion.setDeleted(true);
        promotionRepository.save(promotion);
    }

    // ==================== HELPERS ====================
    private Promotion findOrThrow(Long id) {
        return promotionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi với ID: " + id));
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private void validateDiscountValue(PromotionType type, BigDecimal value) {
        if (type == PromotionType.PERCENTAGE && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Giá trị giảm theo % không được vượt quá 100%");
        }
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

    private PromotionResponse toResponse(Promotion p) {
        LocalDateTime now = LocalDateTime.now();
        boolean available = p.getStatus() == PromotionStatus.ACTIVE
                && !p.getEndDate().isBefore(now)
                && !p.getStartDate().isAfter(now)
                && (p.getUsageLimit() == null || p.getUsedCount() < p.getUsageLimit());

        return PromotionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .type(p.getType())
                .discountValue(p.getDiscountValue())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .minBookingValue(p.getMinBookingValue())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .usageLimit(p.getUsageLimit())
                .usedCount(p.getUsedCount())
                .status(p.getStatus())
                .available(available)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
