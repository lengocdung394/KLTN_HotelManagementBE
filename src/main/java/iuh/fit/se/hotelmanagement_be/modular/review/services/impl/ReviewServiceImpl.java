package iuh.fit.se.hotelmanagement_be.modular.review.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.review.entities.Review;
import iuh.fit.se.hotelmanagement_be.modular.review.repositories.ReviewRepository;
import iuh.fit.se.hotelmanagement_be.modular.review.requests.ReviewCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.review.responses.HotelReviewSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.review.responses.ReviewResponse;
import iuh.fit.se.hotelmanagement_be.modular.review.services.ReviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewServiceImpl implements ReviewService {
    ReviewRepository reviewRepository;
    BookingRepository bookingRepository;
    CustomerRepository customerRepository;
    HotelRepository hotelRepository;
    AccountRepository accountRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        Customer customer = account.getCustomer();
        if (customer == null) {
            customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tài khoản chưa liên kết thông tin khách hàng"));
        }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng #" + request.getBookingId()));

        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("Bạn không phải chủ đơn đặt phòng này");
        }

        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("Chỉ có thể đánh giá sau khi đã hoàn tất kỳ lưu trú");
        }

        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new RuntimeException("Đơn đặt phòng này đã được đánh giá");
        }

        // Xác định khách sạn và loại phòng từ bookingDetails
        Hotel hotel = null;
        String roomTypeName = "Phòng nghỉ tiêu chuẩn";
        if (booking.getBookingDetails() != null && !booking.getBookingDetails().isEmpty()) {
            BookingDetail firstDetail = booking.getBookingDetails().get(0);
            if (firstDetail.getRoom() != null) {
                if (firstDetail.getRoom().getRoomType() != null) {
                    roomTypeName = "Phòng " + firstDetail.getRoom().getRoomType().name();
                }
                if (firstDetail.getRoom().getFloor() != null
                        && firstDetail.getRoom().getFloor().getBuilding() != null) {
                    hotel = firstDetail.getRoom().getFloor().getBuilding().getHotel();
                }
            }
        }

        if (hotel == null) {
            hotel = hotelRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn"));
        }

        Review review = Review.builder()
                .booking(booking)
                .customer(customer)
                .hotel(hotel)
                .roomTypeName(roomTypeName)
                .rating(request.getRating())
                .cleanlinessRating(request.getCleanlinessRating() != null ? request.getCleanlinessRating() : request.getRating())
                .serviceRating(request.getServiceRating() != null ? request.getServiceRating() : request.getRating())
                .facilitiesRating(request.getFacilitiesRating() != null ? request.getFacilitiesRating() : request.getRating())
                .locationRating(request.getLocationRating() != null ? request.getLocationRating() : request.getRating())
                .title(request.getTitle())
                .comment(request.getComment() != null ? request.getComment().trim() : "")
                .createdAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);
        return toReviewResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelReviewSummaryResponse getHotelReviews(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn #" + hotelId));

        List<Review> reviews = reviewRepository.findByHotelIdOrderByCreatedAtDesc(hotelId);
        int total = reviews.size();

        double avgRating = 5.0;
        double avgClean = 5.0;
        double avgService = 5.0;
        double avgFacilities = 5.0;
        double avgLocation = 5.0;

        Map<Integer, Long> starCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) starCounts.put(i, 0L);

        if (total > 0) {
            avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(5.0);
            avgClean = reviews.stream().mapToInt(r -> r.getCleanlinessRating() != null ? r.getCleanlinessRating() : r.getRating()).average().orElse(5.0);
            avgService = reviews.stream().mapToInt(r -> r.getServiceRating() != null ? r.getServiceRating() : r.getRating()).average().orElse(5.0);
            avgFacilities = reviews.stream().mapToInt(r -> r.getFacilitiesRating() != null ? r.getFacilitiesRating() : r.getRating()).average().orElse(5.0);
            avgLocation = reviews.stream().mapToInt(r -> r.getLocationRating() != null ? r.getLocationRating() : r.getRating()).average().orElse(5.0);

            for (Review r : reviews) {
                int star = r.getRating();
                starCounts.put(star, starCounts.getOrDefault(star, 0L) + 1);
            }
        }

        List<ReviewResponse> reviewResponses = reviews.stream()
                .map(this::toReviewResponse)
                .collect(Collectors.toList());

        return HotelReviewSummaryResponse.builder()
                .hotelId(hotel.getId())
                .hotelName(hotel.getName())
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .totalReviews(total)
                .avgCleanliness(Math.round(avgClean * 10.0) / 10.0)
                .avgService(Math.round(avgService * 10.0) / 10.0)
                .avgFacilities(Math.round(avgFacilities * 10.0) / 10.0)
                .avgLocation(Math.round(avgLocation * 10.0) / 10.0)
                .starCounts(starCounts)
                .reviews(reviewResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewByBookingId(String bookingId) {
        return reviewRepository.findByBookingId(bookingId)
                .map(this::toReviewResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        Customer customer = account.getCustomer();
        if (customer == null) {
            customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tài khoản chưa liên kết thông tin khách hàng"));
        }

        return reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(this::toReviewResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse toReviewResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .bookingId(r.getBooking() != null ? r.getBooking().getId() : null)
                .customerId(r.getCustomer() != null ? r.getCustomer().getId() : null)
                .customerName(r.getCustomer() != null ? r.getCustomer().getFullName() : "Khách hàng Sen Việt")
                .hotelId(r.getHotel() != null ? r.getHotel().getId() : null)
                .hotelName(r.getHotel() != null ? r.getHotel().getName() : "")
                .roomTypeName(r.getRoomTypeName())
                .rating(r.getRating())
                .cleanlinessRating(r.getCleanlinessRating())
                .serviceRating(r.getServiceRating())
                .facilitiesRating(r.getFacilitiesRating())
                .locationRating(r.getLocationRating())
                .title(r.getTitle())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
