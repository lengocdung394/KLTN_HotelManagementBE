package iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForRoomExcel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomBedImportRequest {
    @NotNull
    Long bedTypeId;

    @NotNull
    @Min(1)
    Integer quantity;
}
