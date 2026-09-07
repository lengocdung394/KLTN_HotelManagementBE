package iuh.fit.se.hotelmanagement_be.modular.branch.services;

import iuh.fit.se.hotelmanagement_be.modular.branch.responses.BuildingResponse;

import java.util.List;

public interface BuildingService {

    List<BuildingResponse> getBuildingsByHotelId(Long hotelId);
}
