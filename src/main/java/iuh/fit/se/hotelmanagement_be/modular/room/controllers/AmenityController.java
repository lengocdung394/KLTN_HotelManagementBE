package iuh.fit.se.hotelmanagement_be.modular.room.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Amenity;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.AmenityRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.AmenityGetAllResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.AmenityService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/amenities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Amenity", description = "APIs liên quan đến quản lý tiện nghi")
public class AmenityController {
    AmenityService amenityService;

    @GetMapping("/getAll")
    @Operation(summary = "Lấy danh sách tất cả tiện ích")
    public ResponseEntity<ApiResponse<List<AmenityGetAllResponse>>> getAll() {
        List<AmenityGetAllResponse> amenities = amenityService.getAllAmenities();

        return ResponseEntity.ok(ApiResponse.<List<AmenityGetAllResponse>>builder()
                .code(200)
                .message("Lấy danh sách tiện ích thành công!")
                .result(amenities)
                .build());
    }
}
