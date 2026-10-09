package iuh.fit.se.hotelmanagement_be.modular.branch.services;

import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Building;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.BuildingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.BuildingResponse;

import java.util.List;

public interface BuildingService {
    BuildingResponse createBuilding(BuildingCreateRequest building, Long hotelId);
    List<BuildingResponse> getBuildingsByHotelId(Long hotelId);
    BuildingResponse updateBuilding(String buildingId, BuildingCreateRequest request, Long hotelId);
}
