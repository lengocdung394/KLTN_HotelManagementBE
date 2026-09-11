package iuh.fit.se.hotelmanagement_be.modular.room.services;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.AmenityGetAllResponse;

import java.util.List;

public interface AmenityService {
    List<AmenityGetAllResponse> getAllAmenities();

}
