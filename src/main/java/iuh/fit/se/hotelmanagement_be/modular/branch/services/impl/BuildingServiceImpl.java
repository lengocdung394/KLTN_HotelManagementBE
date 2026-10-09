package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import iuh.fit.se.hotelmanagement_be.config.SecurityUtils;
import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Building;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BuildingRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.BuildingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.BuildingResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.BuildingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BuildingServiceImpl implements BuildingService {
    BuildingRepository buildingRepository;
    HotelRepository hotelRepository;
    BuildingFloorSocketEmitter buildingFloorSocketEmitter;

    @Override
    @Transactional
    public BuildingResponse createBuilding(BuildingCreateRequest request, Long hotelId) {
        if (hotelId == null) {
            throw new AppException(ErrorCode.MANAGER_HOTEL_NOT_ASSIGNED);
        }

        String name = request == null || request.getName() == null
                ? ""
                : request.getName().trim().replaceAll("\\s+", " ");
        if (name.isBlank()) {
            throw new AppException(ErrorCode.BUILDING_NAME_REQUIRED);
        }
        if (buildingRepository.findByHotelIdAndNameIgnoreCase(hotelId, name).isPresent()) {
            throw new AppException(ErrorCode.BUILDING_ALREADY_EXISTS);
        }
        if (buildingRepository.existsById(Building.generateId(hotelId, name))) {
            throw new AppException(ErrorCode.BUILDING_ID_ALREADY_EXISTS);
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_FOUND));
        Building building = buildingRepository.save(Building.builder()
                .name(name)
                .hotel(hotel)
                .build());

        BuildingResponse buildingResponse = BuildingResponse.builder()
                .id(building.getId())
                .name(building.getName())
                .build();

        buildingFloorSocketEmitter.emitBuildingChanged(hotelId, "CREATE", buildingResponse);
        return buildingResponse;
    }

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

    @Override
    public BuildingResponse updateBuilding(String buildingId, BuildingCreateRequest request, Long hotelId) {
        if (hotelId == null) {
            throw new AppException(ErrorCode.MANAGER_HOTEL_NOT_ASSIGNED);
        }
        String name = request == null || request.getName() == null
                ? ""
                : request.getName().trim().replaceAll("\\s+", " ");
        if (name.isBlank()) {
            throw new AppException(ErrorCode.BUILDING_NAME_REQUIRED);
        }

        Building building = buildingRepository.findByIdAndHotelId(buildingId, hotelId)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
        String currentBuildingId = building.getId();
        if (buildingRepository.findByHotelIdAndNameIgnoreCase(hotelId, name)
                .filter(existing -> !existing.getId().equals(currentBuildingId))
                .isPresent()) {
            throw new AppException(ErrorCode.BUILDING_ALREADY_EXISTS);
        }

        boolean changed = !building.getName().equals(name);
        if (changed) {
            building.setName(name);
            building = buildingRepository.save(building);
        }

        BuildingResponse response = BuildingResponse.builder()
                .id(building.getId())
                .name(building.getName())
                .build();
        if (changed) {
            emitAfterCommit(() -> buildingFloorSocketEmitter.emitBuildingChanged(hotelId, "UPDATED", response));
        }
        return response;
    }

    private void emitAfterCommit(Runnable emit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("Transaction synchronization is not active; building socket event was not emitted");
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emit.run();
            }
        });
    }
}
