package iuh.fit.se.hotelmanagement_be.modular.branch.services;

import iuh.fit.se.hotelmanagement_be.modular.branch.responses.FloorResponse;

import java.util.List;

public interface FloorService {
    List<FloorResponse> getFloorsByBuildingId(Long buildingId);
}
