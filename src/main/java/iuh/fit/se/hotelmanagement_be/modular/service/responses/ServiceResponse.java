package iuh.fit.se.hotelmanagement_be.modular.service.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponse {
    String id;
    String name;
    String description;
    Double price;
    String unit;
    String category;
    String imageUrl;
    Boolean active;
    Long hotelId;
    String hotelName;
}
