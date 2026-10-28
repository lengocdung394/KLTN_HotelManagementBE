package iuh.fit.se.hotelmanagement_be.modular.room.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AmenityRequest {
    @NotBlank
    String name;

    @NotNull
    @DecimalMin("0.0")
    Double price;
}
