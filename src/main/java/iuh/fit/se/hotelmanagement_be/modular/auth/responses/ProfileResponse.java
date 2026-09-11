package iuh.fit.se.hotelmanagement_be.modular.auth.responses;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {
    Long userId;
    Long accountId;
    String fullName;
    String email;
    String phone;
    String cccd;
    @JsonFormat(pattern = "dd/MM/yyyy")
    LocalDate dateOfBirth;
    String avatarUrl;
    String position;
}