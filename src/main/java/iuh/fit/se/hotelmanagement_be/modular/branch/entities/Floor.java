package iuh.fit.se.hotelmanagement_be.modular.branch.entities;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
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

    /**
     * 💡 Tự động sinh mã tầng theo chuẩn: [buildingId]_F[floorNumber]
     * Ví dụ: Tòa nhà có ID "H1_TOA_A" và tầng số 2 -> ID tự sinh: "H1_TOA_A_F2"
     */
    @PrePersist
    @PreUpdate
    public void generateFloorId() {
        if (this.building != null && this.building.getId() != null) {
            this.id = this.building.getId() + "_F" + this.floorNumber;
        }
    }
}
