package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.FloorRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.FloorResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.FloorService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FloorServiceImpl implements FloorService {
    FloorRepository floorRepository;

    @Override
    public List<FloorResponse> getFloorsByBuildingId(Long buildingId) {
        return floorRepository.findByBuildingId(buildingId).stream().map(f -> FloorResponse.builder()
                        .id(f.getId())
                        .floorNumber(f.getFloorNumber())
                        .build())
                .collect(Collectors.toList());
    }
}
