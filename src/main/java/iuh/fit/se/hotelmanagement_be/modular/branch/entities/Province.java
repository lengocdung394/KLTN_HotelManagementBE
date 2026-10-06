package iuh.fit.se.hotelmanagement_be.modular.branch.entities;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@EqualsAndHashCode()
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "provinces")
public class Province {

    @Id
    @Column(name = "province_id", length = 50, nullable = false)
    String id; // Ví dụ: "HO_CHI_MINH", "HA_NOI", "DA_NANG"

    @Column(name = "name", nullable = false)
    String name; // Tên tỉnh/thành phố (Ví dụ: "Hồ Chí Minh")

    @Column( nullable = false)
    String backgroundImageUrl;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel> hotels;

    /**
     * 💡 Tự động chuẩn hóa tên Tỉnh/Thành phố thành mã viết hoa không dấu, cách nhau bằng gạch dưới.
     * Ví dụ: "Hồ Chí Minh" -> "HO_CHI_MINH"
     */
    @PrePersist
    @PreUpdate
    public void generateProvinceId() {
        if (this.name != null && !this.name.trim().isEmpty()) {
            this.id = removeAccents(this.name).trim().toUpperCase().replaceAll("\\s+", "_");
        }
    }

    /**
     * Hàm phụ trợ loại bỏ dấu tiếng Việt để tạo mã sạch sẽ
     */
    private String removeAccents(String str) {
        try {
            String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
            Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
            return pattern.matcher(temp).replaceAll("").replaceAll("Đ", "D").replaceAll("đ", "d");
        } catch (Exception e) {
            return str;
        }
    }
}