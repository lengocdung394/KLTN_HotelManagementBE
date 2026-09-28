package iuh.fit.se.hotelmanagement_be.modular.review.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelReviewSummaryResponse {
    Long hotelId;
    String hotelName;
    Double averageRating;
    Integer totalReviews;
    Double avgCleanliness;
    Double avgService;
    Double avgFacilities;
    Double avgLocation;
    Map<Integer, Long> starCounts;
    List<ReviewResponse> reviews;
}
