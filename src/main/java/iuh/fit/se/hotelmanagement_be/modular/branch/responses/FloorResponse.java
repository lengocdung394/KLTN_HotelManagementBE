package iuh.fit.se.hotelmanagement_be.modular.branch.responses;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloorResponse {
    Long id;
    String name;
    Integer floorNumber;
    Long buildingId;
}
