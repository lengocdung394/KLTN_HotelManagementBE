package iuh.fit.se.hotelmanagement_be.modular.room.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.BedType;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.BedTypeRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.BedTypeGetAllResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.services.BedTypeService;
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
@RequestMapping("/bedTypes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "BedType", description = "APIs liên quan đến quản lý tiện nghi")
public class BedTypeController {

    BedTypeService  bedTypeService;
    @Operation(summary = "Lay danh sach trang thai phong (RoomStatus)")
    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<List<BedTypeGetAllResponse>>> getAllBedTypes() {

        List<BedTypeGetAllResponse> bedTypeServiceAll = bedTypeService.findAll();

        return ResponseEntity.ok(ApiResponse.<List<BedTypeGetAllResponse>>builder()
                .code(200)
                .message("Lay ds trang thai phong thanh cong")
                .result(bedTypeServiceAll)
                .build()
        );
    }

}
