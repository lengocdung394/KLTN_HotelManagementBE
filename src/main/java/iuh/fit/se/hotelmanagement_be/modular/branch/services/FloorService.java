package iuh.fit.se.hotelmanagement_be.modular.branch.services;

import iuh.fit.se.hotelmanagement_be.modular.branch.requests.FloorCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.FloorUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.FloorResponse;

import java.util.List;

public interface FloorService {
    List<FloorResponse> getFloorsByBuildingId(String buildingId);
    List<FloorResponse> getAllFloors(Long hotelId);
    FloorResponse createFloor(FloorCreateRequest request, Long hotelId);
    FloorResponse updateFloor(String floorId, FloorUpdateRequest request, Long hotelId);
}
