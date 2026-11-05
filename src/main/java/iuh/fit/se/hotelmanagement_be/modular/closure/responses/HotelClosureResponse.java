package iuh.fit.se.hotelmanagement_be.modular.closure.responses;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HotelClosureResponse(
        String id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean allHotels,
        List<Long> hotelIds,
        LocalDateTime createdAt) {
}
