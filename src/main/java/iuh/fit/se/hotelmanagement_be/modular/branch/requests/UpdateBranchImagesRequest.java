package iuh.fit.se.hotelmanagement_be.modular.branch.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record UpdateBranchImagesRequest(
        @NotNull
        List<
                @Pattern(
                        regexp = "^https://res\\.cloudinary\\.com/[^\\s]+$",
                        message = "Ảnh phải là URL HTTPS hợp lệ từ Cloudinary."
                ) String> imageUrls
) {
}
