package iuh.fit.se.hotelmanagement_be.modular.room.services;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.BedType;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.BedTypeRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.BedTypeGetAllResponse;

import java.util.List;

public interface BedTypeService {
    List<BedTypeGetAllResponse> findAll();

    BedTypeGetAllResponse create(BedTypeRequest request);

    BedTypeGetAllResponse update(Long id, BedTypeRequest request);

    List<BedTypeGetAllResponse> importAll(List<BedTypeRequest> requests);
}
