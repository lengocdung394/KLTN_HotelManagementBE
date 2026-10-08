package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportRolePermissionsRequest {

    @NotEmpty
    List<@Valid RolePermissionGroupRequest> rolePermissions;
}
