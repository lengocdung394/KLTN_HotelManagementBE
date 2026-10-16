package iuh.fit.se.hotelmanagement_be.modular.branch.entities;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "floors")
public class Floor {
    @Id
    @Column(name = "floor_id", length = 60, nullable = false)
    String id; // Ví dụ: "H1_B1_F2" (Hotel 1 - Building B1 - Floor 2)

    int floorNumber; // số tầng

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Room> rooms;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "building_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    Building building;

    public static String generateId(String buildingId, int floorNumber) {
        if (buildingId == null) {
            return null;
        }

        String suffix = "_F" + floorNumber;
        String id = buildingId + suffix;
        if (id.length() <= 60) {
            return id;
        }

        String hash = UUID.nameUUIDFromBytes(buildingId.getBytes(StandardCharsets.UTF_8))
                .toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase(Locale.ROOT);
        int buildingPrefixLength = 60 - suffix.length() - hash.length() - 1;
        String buildingPrefix = buildingId.substring(0, buildingPrefixLength).replaceAll("_+$", "");
        return buildingPrefix + "_" + hash + suffix;
    }

    @PrePersist
    public void generateFloorId() {
        if (this.building != null && this.building.getId() != null) {
            this.id = generateId(this.building.getId(), this.floorNumber);
        }
    }
}
