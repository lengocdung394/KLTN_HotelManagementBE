package iuh.fit.se.hotelmanagement_be.modular.promotion.entities;

import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "promotions", indexes = {
        @Index(name = "idx_promotion_code",   columnList = "code",              unique = true),
        @Index(name = "idx_promotion_status", columnList = "status"),
        @Index(name = "idx_promotion_dates",  columnList = "start_date, end_date")
})
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    String code;

    @Column(name = "name", nullable = false, length = 200)
    String name;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    PromotionType type;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    BigDecimal maxDiscountAmount;

    @Column(name = "min_booking_value", precision = 15, scale = 2)
    BigDecimal minBookingValue;

    @Column(name = "start_date", nullable = false)
    LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    LocalDateTime endDate;

    @Column(name = "usage_limit")
    Integer usageLimit;

    @Builder.Default
    @Column(name = "used_count", nullable = false)
    Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    PromotionStatus status = PromotionStatus.DRAFT;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
