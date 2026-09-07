package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import iuh.fit.se.hotelmanagement_be.config.SecurityUtils;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BuildingRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.BuildingResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.BuildingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BuildingServiceImpl implements BuildingService {
    BuildingRepository buildingRepository;


    @Override
    public List<BuildingResponse> getBuildingsByHotelId(Long hotelId) {

        Long currentHotelId = SecurityUtils.getCurrentUserHotelId();
        if (currentHotelId != null) {
            hotelId = currentHotelId;
        }

        return buildingRepository.findByHotelId(hotelId).stream()
                .map(
                b -> BuildingResponse.builder()
                        .name(b.getName())
                        .id(b.getId())
                        .build())
                .collect(Collectors.toList());
    }
}
