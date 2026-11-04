package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import iuh.fit.se.hotelmanagement_be.modular.branch.responses.PublicHotelResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.PublicHotelService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/hotel/public")
@RequiredArgsConstructor
public class PublicHotelController {
    private final PublicHotelService publicHotelService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PublicHotelResponse>>> getHotels() {
        ApiResponse<List<PublicHotelResponse>> response = ApiResponse.<List<PublicHotelResponse>>builder()
                .code(200)
                .message("Lấy danh sách khách sạn thành công.")
                .result(publicHotelService.getHotels())
                .build();
        return ResponseEntity.ok(response);
    }
}
