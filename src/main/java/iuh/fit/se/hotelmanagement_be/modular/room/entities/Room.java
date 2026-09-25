package iuh.fit.se.hotelmanagement_be.modular.room.entities;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Floor;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import jakarta.persistence.*;
import lombok.*;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "room_status")
    RoomStatus roomStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type")
    RoomType roomType;

    @Column(name = "room_number", length = 20)
    String roomNumber;

    @Column(name = "price")
    @Builder.Default
    Double price = 0.0;

    @Column(name = "base_price")
    @Builder.Default
    Double basePrice = 0.0;

    // Tien ich
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_amenities",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    Set<Amenity> amenities;


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

    public Double calculateRoomTotalPrice(Room room, BranchRoomPolicy policy) {
        // 1. Lấy giá cơ bản của loại phòng từ chính sách chi nhánh
        double basePrice = (policy != null && policy.getBasePrice() != null) ? policy.getBasePrice() : 0.0;

        // 2. Cộng thêm tổng tiền tiện ích riêng của căn phòng đó (nếu có)
        double amenitiesPrice = room.getTotalAmenitiesPrice();

        return basePrice + amenitiesPrice;
    }
}
