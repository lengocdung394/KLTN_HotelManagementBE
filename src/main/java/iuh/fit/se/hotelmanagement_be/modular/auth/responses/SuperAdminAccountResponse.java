package iuh.fit.se.hotelmanagement_be.modular.auth.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperAdminAccountResponse {

    String accountId;
    String profileId;
    String accountType;
    String email;
    String fullName;
    String phone;
    String identityNumber;
    String address;
    String position;
    String avatarUrl;
    Long hotelId;
    String hotelName;
    Boolean registered;
    String loyaltyTier;
    Set<String> roles;
}
