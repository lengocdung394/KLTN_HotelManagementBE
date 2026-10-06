package iuh.fit.se.hotelmanagement_be.modular.service.requests.requestForExcel;

import lombok.*;
import lombok.experimental.FieldDefaults;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceExcelRowRequest {

    private Integer rowNumber;
    private String name;
    private String description;
    private Double price;
    private String unit;
    private String category;
    private String imageUrl;
}
