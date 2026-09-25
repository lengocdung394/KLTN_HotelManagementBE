package iuh.fit.se.hotelmanagement_be.modular.review.services;

import iuh.fit.se.hotelmanagement_be.modular.review.requests.ReviewCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.review.responses.HotelReviewSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.review.responses.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(ReviewCreateRequest request);
    HotelReviewSummaryResponse getHotelReviews(Long hotelId);
    ReviewResponse getReviewByBookingId(Long bookingId);
    List<ReviewResponse> getMyReviews();
}
