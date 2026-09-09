package iuh.fit.se.hotelmanagement_be.modular.promotion.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "customer_promotions")
public class CustomerPromotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    Promotion promotion;

    @Column(name = "unique_code", nullable = false, unique = true, length = 100)
    String uniqueCode;

    @Column(name = "is_used")
    boolean isUsed = false;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    Booking booking;

    @PrePersist
    public void autoGenerateUniqueCode() {
        if (this.uniqueCode == null || this.uniqueCode.isBlank()) {
            String prefix = (this.promotion != null && this.promotion.getCode() != null)
                    ? this.promotion.getCode().toUpperCase()
                    : "EXCL";

            // Lấy 8 ký tự ngẫu nhiên từ UUID
            String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            this.uniqueCode = prefix + "-" + randomPart;
        }
    }
}
