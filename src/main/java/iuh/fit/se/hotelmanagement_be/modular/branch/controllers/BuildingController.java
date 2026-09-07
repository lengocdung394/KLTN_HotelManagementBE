package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.BuildingResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.BuildingService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/branch")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Building", description = "APIs liên quan đến quản ly toa nha")
public class BuildingController {
    BuildingService buildingService;

    @GetMapping("/getBuildingByHotelId")
    @Operation(
            summary = "Lấy danh sách tòa theo ID của khách sạn"

    )
    public ResponseEntity<ApiResponse<List<BuildingResponse>>> getBuildingsByHotelId(@RequestParam Long hotelId){
        List<BuildingResponse> buildingResponseList = buildingService.getBuildingsByHotelId(hotelId);
        return ResponseEntity.ok(ApiResponse.<List<BuildingResponse>>builder()
                .code(100)
                .result(buildingResponseList)
                .message("Lay thanh cong danh sach")
                .build()

        );
    }
}
