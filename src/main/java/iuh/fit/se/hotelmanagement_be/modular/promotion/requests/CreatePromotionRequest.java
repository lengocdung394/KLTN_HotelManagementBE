package iuh.fit.se.hotelmanagement_be.modular.promotion.requests;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionDiscountType;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request tạo mới khuyến mãi")
public class CreatePromotionRequest {

    @Column(name = "name", nullable = false, length = 200)
    String name;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    PromotionScope type;


    @Enumerated(EnumType.STRING)
    PromotionDiscountType discountType;

    BigDecimal discountValue;

    BigDecimal maxDiscountAmount; // Số tiền giảm tối đa (chỉ áp dụng khi discountType = PERCENTAGE)
    //(Số tiền giảm giá tối đa)

    BigDecimal minBookingValue;


    BigDecimal minRoomValue;    // Tổng tiền phòng tối thiểu (để kích hoạt mã riêng cho phòng)


    BigDecimal minServiceValue; // Tổng tiền dịch vụ tối thiểu (để kích hoạt mã riêng cho dịch vụ)


    LocalDateTime startDate;


    LocalDateTime endDate;


    Integer usageLimit;



    Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    PromotionStatus status = PromotionStatus.DRAFT;


    @Column(name = "deleted", nullable = false)
    boolean deleted = false;

    @CreationTimestamp

    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;


    boolean isExclusive = false;

    String imageUrl;
}
