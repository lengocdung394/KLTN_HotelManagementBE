package iuh.fit.se.hotelmanagement_be.modular.service.entities;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@EqualsAndHashCode()
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "services")
public class Service {
    @Id
    @Column(name = "service_id")
    String id;

    @Column(nullable = false)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false)
    Double price;

    String unit; // Đơn vị tính: suất, người, lượt, buổi...

    String category; // Phân loại: Nhà hàng, Hội nghị, Spa, Đưa đón...

    String imageUrl;

    @Builder.Default
    Boolean active = true;

    // Chi nhánh áp dụng (nếu null = áp dụng cho tất cả chi nhánh)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    Hotel hotel;

    // --- TỰ ĐỘNG SINH MÃ DỊCH VỤ TRƯỚC KHI LƯU ---
    @PrePersist
    protected void onCreate() {
        if (this.id == null || this.id.isEmpty()) {
            String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
            int randomNum = (int) (Math.random() * 9000) + 1000;
            this.id = "SRV" + dateStr + randomNum; // Ví dụ: SRV202609228492
        }
    }
}
