package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;

public interface BookingService {
    BookingResponse createBooking(BookingCreateRequest request);
    BookingResponse createCounterBooking(Long employeeId, BookingCreateRequest request);
}
