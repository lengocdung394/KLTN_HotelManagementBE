package iuh.fit.se.hotelmanagement_be.modular.room.services;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.BedType;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.BedTypeGetAllResponse;

import java.util.List;

public interface BedTypeService {
    List<BedTypeGetAllResponse> findAll();
}
