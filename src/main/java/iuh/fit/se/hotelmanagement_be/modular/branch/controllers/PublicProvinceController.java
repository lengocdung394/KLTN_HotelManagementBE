package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.ProvinceRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.PublicProvinceResponse;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/province/public")
@RequiredArgsConstructor
public class PublicProvinceController {
    private final ProvinceRepository provinceRepository;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PublicProvinceResponse>>> getPublicProvinces() {
        List<PublicProvinceResponse> provinces = provinceRepository.findAll().stream()
                .map(province -> PublicProvinceResponse.builder()
                        .id(province.getId())
                        .name(province.getName())
                        .backgroundImageUrl(province.getBackgroundImageUrl())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.<List<PublicProvinceResponse>>builder()
                .code(200)
                .message("Lấy danh sách tỉnh/thành công.")
                .result(provinces)
                .build());
    }
}
