package iuh.fit.se.hotelmanagement_be.modular.review.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {
    Long id;
    Long bookingId;
    Long customerId;
    String customerName;
    Long hotelId;
    String hotelName;
    String roomTypeName;
    Integer rating;
    Integer cleanlinessRating;
    Integer serviceRating;
    Integer facilitiesRating;
    Integer locationRating;
    String title;
    String comment;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    LocalDateTime createdAt;
}
