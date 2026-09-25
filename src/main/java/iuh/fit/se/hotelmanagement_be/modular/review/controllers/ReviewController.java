package iuh.fit.se.hotelmanagement_be.modular.review.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.review.requests.ReviewCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.review.responses.HotelReviewSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.review.responses.ReviewResponse;
import iuh.fit.se.hotelmanagement_be.modular.review.services.ReviewService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Reviews", description = "APIs đánh giá & xếp hạng khách sạn")
public class ReviewController {
    ReviewService reviewService;

    @Operation(summary = "Tạo đánh giá cho đơn đặt phòng hoàn tất")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.ok(ApiResponse.<ReviewResponse>builder()
                .code(1000)
                .message("Đã gửi đánh giá thành công")
                .result(response)
                .build());
    }

    @Operation(summary = "Lấy tổng hợp đánh giá và danh sách nhận xét của khách sạn")
    @GetMapping("/hotels/{hotelId}")
    public ResponseEntity<ApiResponse<HotelReviewSummaryResponse>> getHotelReviews(@PathVariable Long hotelId) {
        HotelReviewSummaryResponse response = reviewService.getHotelReviews(hotelId);
        return ResponseEntity.ok(ApiResponse.<HotelReviewSummaryResponse>builder()
                .code(1000)
                .message("Lấy danh sách đánh giá thành công")
                .result(response)
                .build());
    }

    @Operation(summary = "Kiểm tra đánh giá theo mã booking")
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewByBookingId(@PathVariable String bookingId) {
        ReviewResponse response = reviewService.getReviewByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.<ReviewResponse>builder()
                .code(1000)
                .message("Lấy thông tin đánh giá thành công")
                .result(response)
                .build());
    }

    @Operation(summary = "Lấy danh sách đánh giá của khách hàng đang đăng nhập")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews() {
        List<ReviewResponse> response = reviewService.getMyReviews();
        return ResponseEntity.ok(ApiResponse.<List<ReviewResponse>>builder()
                .code(1000)
                .message("Lấy danh sách đánh giá cá nhân thành công")
                .result(response)
                .build());
    }
}
