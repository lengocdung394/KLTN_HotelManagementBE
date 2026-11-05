package iuh.fit.se.hotelmanagement_be.modular.closure.responses;

import java.time.LocalDate;

public record PublicHotelClosureResponse(
        String id,
        LocalDate startDate,
        LocalDate endDate) {
}
