package iuh.fit.se.hotelmanagement_be.modular.service.entities;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    Long id;

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
}
