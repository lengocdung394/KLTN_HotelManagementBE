package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionCatalogItemRequest {

    @NotBlank
    String code;

    @NotBlank
    String name;

    String description;
}
