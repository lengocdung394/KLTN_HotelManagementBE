package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.UpdateBranchImagesRequest;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/hotel/current")
@RequiredArgsConstructor
public class BranchHotelImageController {
    private static final int MAX_IMAGES = 10;

    private final HotelRepository hotelRepository;

    @GetMapping("/images")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<String>>> getBranchImages(Authentication authentication) {
        Hotel hotel = getCurrentHotel(authentication);
        List<String> imageUrls = hotel.getImageUrls() == null ? List.of() : List.copyOf(hotel.getImageUrls());
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .code(200)
                .message("Lấy hình ảnh chi nhánh thành công.")
                .result(imageUrls)
                .build());
    }

    @PutMapping("/images")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<String>>> updateBranchImages(
            @Valid @RequestBody UpdateBranchImagesRequest request,
            Authentication authentication) {
        if (request.imageUrls().size() > MAX_IMAGES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mỗi chi nhánh được cấu hình tối đa " + MAX_IMAGES + " ảnh.");
        }

        Hotel hotel = getCurrentHotel(authentication);
        List<String> imageUrls = request.imageUrls().stream()
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        hotel.setImageUrls(imageUrls);
        hotelRepository.save(hotel);

        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .code(200)
                .message("Cập nhật hình ảnh chi nhánh thành công.")
                .result(imageUrls)
                .build());
    }

    private Hotel getCurrentHotel(Authentication authentication) {
        Account account = (Account) authentication.getPrincipal();
        Long hotelId = account.getHotelId();
        if (hotelId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản chưa được gán chi nhánh.");
        }
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chi nhánh."));
    }
}
