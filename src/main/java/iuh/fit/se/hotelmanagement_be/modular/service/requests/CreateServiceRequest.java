package iuh.fit.se.hotelmanagement_be.modular.service.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Yêu cầu tạo mới dịch vụ khách sạn")
public class CreateServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Schema(example = "Buffet sáng thượng hạng")
    String name;

    @Schema(example = "Phục vụ buffet sáng đa dạng ẩm thực Á - Âu từ 06:00 đến 10:00")
    String description;

    @NotNull(message = "Giá dịch vụ không được để trống")
    @Min(value = 0, message = "Giá dịch vụ phải lớn hơn hoặc bằng 0")
    @Schema(example = "250000.0")
    Double price;

    @NotBlank(message = "Đơn vị tính không được để trống (suất, người, lượt, buổi...)")
    @Schema(example = "người")
    String unit;

    @NotBlank(message = "Loại dịch vụ không được để trống (Nhà hàng, Hội nghị, Spa...)")
    @Schema(example = "restaurant")
    String category;

    @Schema(example = "https://images.unsplash.com/photo-1552566626-52f8b828?q=80&w=700")
    String imageUrl;

    @Schema(description = "ID khách sạn áp dụng (để trống nếu áp dụng cho toàn bộ chi nhánh)", example = "1")
    Long hotelId;
}
