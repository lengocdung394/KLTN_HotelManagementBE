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

    @JdbcTypeCode(SqlTypes.JSON)   // Hibernate 6+
    @Column(columnDefinition = "json")
    List<String> avatarUrl;

    @JoinColumn(name = "floor_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    Floor floor;

    RoomStatus roomStatus;
    RoomType roomType;

    // Tien ich
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<Amenity> amenities;

    double price;

}
