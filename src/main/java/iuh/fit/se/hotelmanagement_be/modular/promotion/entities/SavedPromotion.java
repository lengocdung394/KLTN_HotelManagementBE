package iuh.fit.se.hotelmanagement_be.modular.promotion.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "saved_promotions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_account_promotion",
                columnNames = {"account_id", "promotion_id"}
        ))
public class SavedPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    Promotion promotion;

    @CreationTimestamp
    @Column(name = "saved_at", updatable = false)
    LocalDateTime savedAt;
}