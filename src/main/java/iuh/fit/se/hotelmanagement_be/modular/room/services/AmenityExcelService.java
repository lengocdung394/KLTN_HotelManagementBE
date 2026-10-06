package iuh.fit.se.hotelmanagement_be.modular.room.services;

import iuh.fit.se.hotelmanagement_be.modular.room.requests.requestForAmenityExcel.AmenityExcelImportRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.AmenityGetAllResponse;
import iuh.fit.se.hotelmanagement_be.shared.entities.ImportTaskStatus;

import java.util.List;

public interface AmenityExcelService {
    String startAsyncImport(AmenityExcelImportRequest request);
    List<AmenityGetAllResponse> importAmenities(AmenityExcelImportRequest request);
    ImportTaskStatus getImportStatus(String taskId);
}
