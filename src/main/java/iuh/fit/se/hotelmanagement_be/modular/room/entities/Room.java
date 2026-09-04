package iuh.fit.se.hotelmanagement_be.modular.room.entities;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Floor;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Set;

@EqualsAndHashCode()
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    List<RoomImage> avatarUrl;

    @JoinColumn(name = "floor_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    Floor floor;

    RoomStatus roomStatus;
    RoomType roomType;

    // Tien ich
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_amenities",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    Set<Amenity> amenities;

    double basePrice;

    // 💡 Hàm Helper tự động tính tổng tiền tất cả tiện ích có trong phòng
    public Double getTotalAmenitiesPrice() {
        if (amenities == null || amenities.isEmpty()) {
            return 0.0;
        }
        return amenities.stream()
                .mapToDouble(amenity -> amenity.getPrice() != null ? amenity.getPrice() : 0.0)
                .sum();
    }

    public String getDefaultImageUrl() {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return "https://images.unsplash.com/photo-1611892440504-42a792e24d32?q=80&w=1000"; // Link ảnh backup
        }
        return avatarUrl.stream()
                .filter(RoomImage::getIsDefault)
                .map(RoomImage::getUrl)
                .findFirst()
                .orElse(avatarUrl.get(0).getUrl()); // Nếu không có cái nào isDefault=true thì lấy ảnh đầu tiên
    }

    // 💡 Hàm Helper tính Tổng giá phòng thực tế (Giá gốc + Tiện ích)
    public Double calculateTotalPrice() {
        return (basePrice != 0 ? basePrice : 0.0) + getTotalAmenitiesPrice();
    }
}
