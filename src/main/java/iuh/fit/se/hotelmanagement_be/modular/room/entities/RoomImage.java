package iuh.fit.se.hotelmanagement_be.modular.room.entities;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomImage {
    String url;
    Boolean isDefault;
}
