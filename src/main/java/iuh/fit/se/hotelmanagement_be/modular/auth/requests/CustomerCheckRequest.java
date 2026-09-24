package iuh.fit.se.hotelmanagement_be.modular.auth.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request doi mat khau")
public class CustomerCheckRequest {

    String fullName;
    String cccd;
    String phone;
}
