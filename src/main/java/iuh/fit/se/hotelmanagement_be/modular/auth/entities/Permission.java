package iuh.fit.se.hotelmanagement_be.modular.auth.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "permissions")
@EqualsAndHashCode()
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    // Ví dụ: BOOKING_VIEW
    @Column(nullable = false, unique = true)
    private String code;

    // Ví dụ: Xem đặt phòng
    @Column(nullable = false)
    private String name;

    private String description;


}