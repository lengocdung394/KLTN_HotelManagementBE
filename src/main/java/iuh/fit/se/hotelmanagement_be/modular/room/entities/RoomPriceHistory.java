package iuh.fit.se.hotelmanagement_be.modular.room.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "room_price_histories")
public class RoomPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // 1. Nhúng mối quan hệ tới khung giá bị thay đổi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seasonal_rate_id", nullable = false)
    RoomSeasonalRate seasonalRate;

    // 2. Nhúng mối quan hệ tới tài khoản/nhân viên thực hiện thay đổi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    Account account; // Hoặc Employee account;

    @Column(name = "old_price", nullable = false)
    Double oldPrice; // Mức giá trước khi sửa

    @Column(name = "new_price", nullable = false)
    Double newPrice; // Mức giá mới sau khi sửa

    @Column(name = "changed_at", nullable = false)
    LocalDateTime changedAt; // Thời điểm thực hiện thay đổi
}
