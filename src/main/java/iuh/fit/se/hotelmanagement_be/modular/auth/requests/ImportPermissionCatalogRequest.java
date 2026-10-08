package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Permission;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportPermissionCatalogRequest {
    @NotEmpty
    List<PermissionCatalogItemRequest> permissions;
}
