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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    int floorNumber;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Room> rooms;


    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "building_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    Building building;
}
