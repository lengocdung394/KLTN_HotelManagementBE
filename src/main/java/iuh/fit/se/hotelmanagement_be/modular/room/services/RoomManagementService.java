package iuh.fit.se.hotelmanagement_be.modular.room.services;

import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;

import java.time.LocalDate;
import java.util.Map;

public interface RoomManagementService {
    Map<String, Map<LocalDate, Double>> getAllRoomsDailyPricesByBranch(Long hotelId, LocalDate startDate, LocalDate endDate);
}
