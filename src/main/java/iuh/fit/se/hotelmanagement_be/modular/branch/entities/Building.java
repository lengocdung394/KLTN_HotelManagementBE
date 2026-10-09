package iuh.fit.se.hotelmanagement_be.modular.branch.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@EqualsAndHashCode(callSuper = false)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "buildings")
public class Building {
    @Id
    @Column(name = "building_id", length = 50, nullable = false)
    String id; // Ví dụ: "H1_B1" (Hotel 1 - Building 1) hoặc tự sinh theo logic

    String name; //  tên toàn nhà

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Floor> floors;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "hotel_id")
    @ManyToOne(fetch = FetchType.LAZY)
    Hotel hotel;
    public static String generateId(Long hotelId, String name) {
        if (hotelId == null || name == null) {
            return null;
        }

        String normalizedName = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toUpperCase(Locale.ROOT);
        String slug = normalizedName.replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (slug.isEmpty()) {
            slug = "BUILDING_" + Integer.toUnsignedString(name.trim().hashCode(), 36)
                    .toUpperCase(Locale.ROOT);
        }

        String prefix = "H" + hotelId + "_";
        int maximumSlugLength = 50 - prefix.length();
        if (slug.length() > maximumSlugLength) {
            String suffix = "_" + Integer.toUnsignedString(slug.hashCode(), 36).toUpperCase(Locale.ROOT);
            int prefixLength = maximumSlugLength - suffix.length();
            slug = slug.substring(0, prefixLength).replaceAll("_+$", "") + suffix;
        }

        return prefix + slug;
    }

    @PrePersist
    public void generateBuildingId() {
        if (this.hotel != null && this.hotel.getId() != null && this.name != null) {
            this.id = generateId(this.hotel.getId(), this.name);
        }
    }
}
