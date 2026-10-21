package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingDetailRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.HoldRoomRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.HoldRoomResponse;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemporaryRoomHoldService {

    private static final long HOLD_MINUTES = 3;

    private final RoomRepository roomRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomRedisLockService roomRedisLockService;
    private final BookingSocketEmitter bookingSocketEmitter;

    public HoldRoomResponse hold(HoldRoomRequest request) {
        if (request.roomId() == null || request.roomId().isBlank()
                || request.checkInTime() == null
                || request.checkOutTime() == null
                || !request.checkOutTime().isAfter(request.checkInTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thông tin phòng hoặc ngày lưu trú không hợp lệ."
            );
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy phòng."
                ));
        if (room.getRoomStatus() == RoomStatus.MAINTENANCE
                || room.getRoomStatus() == RoomStatus.CLEANING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Phòng đang bảo trì hoặc dọn dẹp nên không thể giữ chỗ."
            );
        }

        LocalDate checkIn = request.checkInTime().toLocalDate();
        LocalDate checkOut = request.checkOutTime().toLocalDate();

        long conflicts = bookingDetailRepository.countOverlappingBookings(
                room.getId(),
                request.checkInTime(),
                request.checkOutTime()
        );

        if (conflicts > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Phòng đã có booking trong khoảng ngày này."
            );
        }

        String lockToken = UUID.randomUUID().toString();
        boolean locked = roomRedisLockService.lockRoomByDateRange(
                room.getId(),
                checkIn,
                checkOut,
                lockToken,
                HOLD_MINUTES
        );

        if (!locked) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Phòng đang được người khác giữ."
            );
        }

        Long hotelId = room.getFloor().getBuilding().getHotel().getId();
        Instant expiresAt = Instant.now().plusSeconds(HOLD_MINUTES * 60);

        bookingSocketEmitter.emitRoomTemporarilyLocked(
                hotelId,
                room.getId(),
                checkIn,
                checkOut,
                lockToken,
                expiresAt
        );

        return new HoldRoomResponse(
                lockToken, room.getId(), checkIn, checkOut, expiresAt
        );
    }

    public void release(
            String roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            String lockToken
    ) {
        roomRedisLockService.releaseRoomLockByDateRange(
                roomId, checkIn, checkOut, lockToken
        );

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy phòng."
                ));

        Long hotelId = room.getFloor().getBuilding().getHotel().getId();
        bookingSocketEmitter.emitRoomTemporarilyUnlocked(
                hotelId, roomId, checkIn, checkOut, lockToken
        );
    }
}
