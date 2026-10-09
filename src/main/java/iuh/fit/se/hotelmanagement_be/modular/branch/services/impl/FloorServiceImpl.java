package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Building;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Floor;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BuildingRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.FloorRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.FloorCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.FloorUpdateRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.BuildingResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.FloorResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.FloorService;
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
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FloorServiceImpl implements FloorService {
    FloorRepository floorRepository;
    BuildingRepository buildingRepository;
    BuildingFloorSocketEmitter buildingFloorSocketEmitter;

    @Override
    public List<FloorResponse> getFloorsByBuildingId(String buildingId) {
        return floorRepository.findByBuildingId(buildingId).stream().map(f -> FloorResponse.builder()
                        .id(f.getId())
                        .floorNumber(f.getFloorNumber())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<FloorResponse> getAllFloors(Long hotelId) {
        return floorRepository.findByBuilding_Hotel_Id(hotelId).stream().map(
                f -> FloorResponse.builder()
                        .id(f.getId())
                        .floorNumber(f.getFloorNumber())
                        .building(BuildingResponse.builder()
                                .id(f.getBuilding().getId())     // Lấy ID của building từ quan hệ f.getBuilding()
                                .name(f.getBuilding().getName()) // Lấy tên building
                                .build())
                        .build()
        ).collect(Collectors.toList());
    }

    @Override
    public FloorResponse createFloor(FloorCreateRequest request, Long hotelId) {
        if (hotelId == null) {
            throw new AppException(ErrorCode.MANAGER_HOTEL_NOT_ASSIGNED);
        }
        if (request == null || request.getBuildingId() == null || request.getBuildingId().isBlank()) {
            throw new AppException(ErrorCode.BUILDING_ID_REQUIRED);
        }
        if (request.getFloorNumber() == null) {
            throw new AppException(ErrorCode.FLOOR_NUMBER_REQUIRED);
        }

        Building building = buildingRepository.findByIdAndHotelId(request.getBuildingId(), hotelId)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
        int floorNumber = request.getFloorNumber();
        if (floorRepository.existsByBuilding_IdAndFloorNumber(building.getId(), floorNumber)) {
            throw new AppException(ErrorCode.FLOOR_ALREADY_EXISTS);
        }

        Floor floor = floorRepository.save(Floor.builder()
                .building(building)
                .floorNumber(floorNumber)
                .build());


        FloorResponse floorResponse = FloorResponse.builder()
                .id(floor.getId())
                .floorNumber(floor.getFloorNumber())
                .building(BuildingResponse.builder()
                        .id(building.getId())
                        .name(building.getName())
                        .build())
                .build();

        buildingFloorSocketEmitter.emitFloorChanged(hotelId, "CREATE", floorNumber);
        return floorResponse;
    }


    @Override
    @Transactional
    public FloorResponse updateFloor(String floorId, FloorUpdateRequest request, Long hotelId) {
        if (hotelId == null) {
            throw new AppException(ErrorCode.MANAGER_HOTEL_NOT_ASSIGNED);
        }
        if (request == null || request.getFloorNumber() == null) {
            throw new AppException(ErrorCode.FLOOR_NUMBER_REQUIRED);
        }

        Floor floor = floorRepository.findByIdAndBuilding_Hotel_Id(floorId, hotelId)
                .orElseThrow(() -> new AppException(ErrorCode.FLOOR_NOT_FOUND));
        int floorNumber = request.getFloorNumber();
        if (floorRepository.existsByBuilding_IdAndFloorNumber(floor.getBuilding().getId(), floorNumber)
                && floor.getFloorNumber() != floorNumber) {
            throw new AppException(ErrorCode.FLOOR_NUMBER_ALREADY_EXISTS);
        }

        boolean changed = floor.getFloorNumber() != floorNumber;
        if (changed) {
            floor.setFloorNumber(floorNumber);
            floor = floorRepository.save(floor);
        }

        Building building = floor.getBuilding();
        FloorResponse response = FloorResponse.builder()
                .id(floor.getId())
                .floorNumber(floor.getFloorNumber())
                .building(BuildingResponse.builder()
                        .id(building.getId())
                        .name(building.getName())
                        .build())
                .build();
        if (changed) {
            emitAfterCommit(() -> buildingFloorSocketEmitter.emitFloorChanged(hotelId, "UPDATED", response));
        }
        return response;
    }

    private void emitAfterCommit(Runnable emit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("Transaction synchronization is not active; floor socket event was not emitted");
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
