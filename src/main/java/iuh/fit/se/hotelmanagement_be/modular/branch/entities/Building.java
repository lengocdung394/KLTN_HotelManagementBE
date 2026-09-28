package iuh.fit.se.hotelmanagement_be.modular.branch.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

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
    /**
     * 💡 Tự động sinh mã tòa nhà theo chuẩn: H[hotelId]_[tên hoặc số thứ tự viết gọn]
     * Ví dụ: Khách sạn ID 1, tòa tên "Tòa A" -> ID tự sinh: "H1_TOA_A" hoặc bạn có thể custom theo ý muốn.
     */
    @PrePersist
    @PreUpdate
    public void generateBuildingId() {
        if (this.hotel != null && this.hotel.getId() != null && this.name != null) {
            // Chuyển tên tòa thành chữ hoa, bỏ dấu hoặc thay khoảng trắng bằng gạch dưới để làm mã gọn gàng
            String normalizedName = this.name.trim().toUpperCase().replaceAll("\\s+", "_");
            this.id = "H" + this.hotel.getId() + "_" + normalizedName;
        }
    }
}
