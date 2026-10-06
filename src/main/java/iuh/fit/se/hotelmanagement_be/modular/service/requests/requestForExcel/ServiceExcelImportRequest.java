package iuh.fit.se.hotelmanagement_be.modular.service.requests.requestForExcel;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceExcelImportRequest {

    List<ServiceExcelRowRequest> services;
}
