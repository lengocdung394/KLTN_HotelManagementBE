package iuh.fit.se.hotelmanagement_be.modular.branch.responses;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Building;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloorResponse {
    String id;
    String name;
    Integer floorNumber;
    BuildingResponse building;

}
