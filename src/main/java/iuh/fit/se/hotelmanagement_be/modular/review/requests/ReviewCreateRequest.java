package iuh.fit.se.hotelmanagement_be.modular.review.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Yêu cầu tạo đánh giá kỳ nghỉ")
public class ReviewCreateRequest {
    @NotNull(message = "Mã đặt phòng không được để trống")
    String bookingId;

    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Điểm đánh giá tối đa là 5 sao")
    Integer rating;

    @Min(1) @Max(5)
    Integer cleanlinessRating;

    @Min(1) @Max(5)
    Integer serviceRating;

    @Min(1) @Max(5)
    Integer facilitiesRating;

    @Min(1) @Max(5)
    Integer locationRating;

    String title;

    String comment;
}
