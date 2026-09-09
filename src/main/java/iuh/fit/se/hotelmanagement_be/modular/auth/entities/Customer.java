package iuh.fit.se.hotelmanagement_be.modular.auth.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.enums.LoyaltyTier;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_phone", columnList = "phone"),
        @Index(name = "idx_customer_email", columnList = "email"),
        @Index(name = "idx_customer_cccd", columnList = "cccd")
})
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    Long id;
    String fullName;
    String phone;
    String email;
    String avatarUrl;
    String cccd;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    Account account;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Booking> bookings;

    @Enumerated(EnumType.STRING)
    @Column(name = "loyalty_tier", nullable = false)
    @Builder.Default
    LoyaltyTier loyaltyTier = LoyaltyTier.BRONZE;

    @Column(name = "total_spent")
    @Builder.Default
    Double totalSpent = 0.0;

    @Column(name = "total_bookings")
    @Builder.Default
    int totalBookings = 0;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    List<CustomerPromotion> customerPromotions;
}
