package iuh.fit.se.hotelmanagement_be.modular.auth.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RolePermissionOverviewResponse {

    String roleCode;
    List<PermissionAssignmentResponse> permissions;
}
