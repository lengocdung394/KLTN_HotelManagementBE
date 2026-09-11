package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "booking_details")
public class BookingDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_detail_id")
    Long id;

    LocalDateTime checkinTime;
    LocalDateTime checkoutTime;
    // Lưu giá tiền phòng
    Double price;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    Booking booking;


    @OneToMany(mappedBy = "bookingDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    List<BookingServiceDetail> bookingServiceDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    Room room;


    Integer numAdults;


    Integer numChildren;


    Integer numInfants;
}
