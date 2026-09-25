package iuh.fit.se.hotelmanagement_be.modular.room.controllers;

import iuh.fit.se.hotelmanagement_be.modular.room.services.RoomManagementService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/management-rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomManagementController {
    RoomManagementService roomManagementService;

    // GET/rooms/prices/branch-all-types?hotelId=1&startDate=2026-09-01&endDate=2026-09-30
    @GetMapping("/branch-prices")
    public ResponseEntity<Map<String, Map<LocalDate, Double>>> getAllRoomsPricesByBranch(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Map<String, Map<LocalDate, Double>> prices = roomManagementService.getAllRoomsDailyPricesByBranch(hotelId, startDate, endDate);
        return ResponseEntity.ok(prices);
    }
}
