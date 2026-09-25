package iuh.fit.se.hotelmanagement_be.modular.review.entities;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = false)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @Column(name = "room_type_name")
    String roomTypeName;

    @Column(nullable = false)
    Integer rating;

    @Column(name = "cleanliness_rating")
    Integer cleanlinessRating;

    @Column(name = "service_rating")
    Integer serviceRating;

    @Column(name = "facilities_rating")
    Integer facilitiesRating;

    @Column(name = "location_rating")
    Integer locationRating;

    String title;

    @Column(columnDefinition = "TEXT")
    String comment;

    @Column(name = "created_at")
    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.cleanlinessRating == null) this.cleanlinessRating = this.rating;
        if (this.serviceRating == null) this.serviceRating = this.rating;
        if (this.facilitiesRating == null) this.facilitiesRating = this.rating;
        if (this.locationRating == null) this.locationRating = this.rating;
    }
}
