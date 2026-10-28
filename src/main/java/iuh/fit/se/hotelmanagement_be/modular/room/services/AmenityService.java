package iuh.fit.se.hotelmanagement_be.modular.room.services;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.AmenityRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.AmenityGetAllResponse;

import java.util.List;

public interface AmenityService {
    List<AmenityGetAllResponse> getAllAmenities();

    AmenityGetAllResponse createAmenity(AmenityRequest request);

    AmenityGetAllResponse updateAmenity(Long id, AmenityRequest request);

}
