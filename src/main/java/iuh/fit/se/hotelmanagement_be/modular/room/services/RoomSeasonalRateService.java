package iuh.fit.se.hotelmanagement_be.modular.room.services;


import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.RoomSeasonalRateCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomSeasonalRateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RoomSeasonalRateService {
    List<RoomSeasonalRateResponse> createSeasonalRate(List<RoomSeasonalRateCreateRequest> requests, Account currentAdmin);
    RoomSeasonalRateResponse updateSeasonalRatePrice(Long rateId, Double newPrice, Account currentAdmin);
    Page<RoomSeasonalRate> getRatesByDate(Long hotelId, RoomType roomType, LocalDate date, Pageable pageable);
}
