package iuh.fit.se.hotelmanagement_be.modular.promotion.entities;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionDiscountType;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionScope;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "promotions", indexes = {
        @Index(name = "idx_promotion_code", columnList = "code", unique = true),
        @Index(name = "idx_promotion_status", columnList = "status"),
        @Index(name = "idx_promotion_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_promotion_hotel", columnList = "hotel_id")
})
public class Promotion {

    @Id
    String id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    String code;

    @Column(name = "name", nullable = false, length = 200)
    String name;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    PromotionScope type;


    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    PromotionDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    BigDecimal maxDiscountAmount; // Số tiền giảm tối đa (chỉ áp dụng khi discountType = PERCENTAGE)
    //(Số tiền giảm giá tối đa)
    @Column(name = "min_booking_value", precision = 15, scale = 2)
    BigDecimal minBookingValue;

    @Column(name = "min_room_value", precision = 15, scale = 2)
    BigDecimal minRoomValue;    // Tổng tiền phòng tối thiểu (để kích hoạt mã riêng cho phòng)

    @Column(name = "min_service_value", precision = 15, scale = 2)
    BigDecimal minServiceValue; // Tổng tiền dịch vụ tối thiểu (để kích hoạt mã riêng cho dịch vụ)

    @Column(name = "start_date", nullable = false)
    LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    LocalDateTime endDate;

    @Column(name = "usage_limit") // so luong  khuyen mai duoc tung ra
    Integer usageLimit;

    @Builder.Default
    @Column(name = "used_count", nullable = false) // so luot khuyen mai da duoc dung
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

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    List<CustomerPromotion> customerPromotions;


    @Column(name = "is_exclusive")
    boolean isExclusive = false;


    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = true) // Nullable = true để hỗ trợ khuyến mãi toàn hệ thống
    Hotel hotel;

    @Column(name = "image_url")
    String imageUrl;

    // --- TỰ ĐỘNG SINH MÃ KHUYẾN MÃI TRƯỚC KHI LƯU ---
    @PrePersist
    protected void onCreate() {
        if (this.id == null || this.id.isEmpty()) {
            String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
            int randomNum = (int) (Math.random() * 9000) + 1000;
            this.id = "PRO" + dateStr + randomNum; // Ví dụ: PRO202609228492
        }
    }
}
