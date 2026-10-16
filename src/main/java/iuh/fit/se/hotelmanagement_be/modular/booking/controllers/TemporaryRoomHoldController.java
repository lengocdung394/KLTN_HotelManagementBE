package iuh.fit.se.hotelmanagement_be.modular.booking.controllers;

import iuh.fit.se.hotelmanagement_be.modular.booking.requests.HoldRoomRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.HoldRoomResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.impl.TemporaryRoomHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms/temporary-locks")
public class TemporaryRoomHoldController {
    private final TemporaryRoomHoldService temporaryRoomHoldService;

    @PostMapping
    public HoldRoomResponse hold(@RequestBody HoldRoomRequest request) {
        return temporaryRoomHoldService.hold(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(
            @RequestParam String roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam String lockToken
    ) {
        temporaryRoomHoldService.release(roomId, checkIn, checkOut, lockToken);
    }
}
