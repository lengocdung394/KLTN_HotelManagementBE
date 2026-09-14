package iuh.fit.se.hotelmanagement_be.modular.room.services;


import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.room.requests.RoomSeasonalRateCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.room.responses.RoomSeasonalRateResponse;

public interface RoomSeasonalRateService {
    RoomSeasonalRateResponse createSeasonalRate(RoomSeasonalRateCreateRequest request, Account currentAdmin);
    RoomSeasonalRateResponse updateSeasonalRatePrice(Long rateId, Double newPrice, Account currentAdmin);
}
