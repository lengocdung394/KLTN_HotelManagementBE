package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Building;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BuildingRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.FloorRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.FloorCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FloorServiceImplTest {
    @Mock
    FloorRepository floorRepository;

    @Mock
    BuildingRepository buildingRepository;

    @Mock
    BuildingFloorSocketEmitter buildingFloorSocketEmitter;

    @InjectMocks
    FloorServiceImpl floorService;

    @Test
    void rejectsFloorWhenGeneratedIdAlreadyExists() {
        Building building = Building.builder()
                .id("H4_TOA_A")
                .build();
        when(buildingRepository.findByIdAndHotelId("H4_TOA_A", 4L))
                .thenReturn(Optional.of(building));
        when(floorRepository.existsByBuilding_IdAndFloorNumber("H4_TOA_A", 3))
                .thenReturn(false);
        when(floorRepository.existsById("H4_TOA_A_F3"))
                .thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> floorService.createFloor(
                        FloorCreateRequest.builder()
                                .buildingId("H4_TOA_A")
                                .floorNumber(3)
                                .build(),
                        4L
                )
        );

        assertEquals(ErrorCode.FLOOR_ALREADY_EXISTS, exception.getErrorCode());
        verify(floorRepository).existsById("H4_TOA_A_F3");
    }
}
