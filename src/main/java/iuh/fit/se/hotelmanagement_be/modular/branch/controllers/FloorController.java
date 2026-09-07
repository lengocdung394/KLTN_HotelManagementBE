package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.FloorResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.FloorService;
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
@RequestMapping("/floor")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Floor", description = "APIs liên quan đến quản lý tang")
public class FloorController {
    FloorService floorService;

    @GetMapping("/getFloorsByBuildingId")
    @Operation(
            summary = "Lấy danh sách tòa nhà theo ID của tòa"
    )
    public ResponseEntity<ApiResponse<List<FloorResponse>>> getFloorsByBuildingId(@RequestParam Long buildingId) {
        List<FloorResponse> floorResponseList = floorService.getFloorsByBuildingId(buildingId);
        return ResponseEntity.ok(ApiResponse.<List<FloorResponse>>builder()
                .code(200)
                .result(floorResponseList)
                .build()

        );
    }

}
