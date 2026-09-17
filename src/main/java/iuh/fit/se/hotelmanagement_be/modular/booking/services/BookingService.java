package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.CheckoutSummaryResponse;

import java.util.List;

public interface BookingService {
    BookingResponse createCustomerBooking(BookingCreateRequest request);
    BookingResponse createCounterBooking(Long employeeId, BookingCreateRequest request);
    BookingResponse toBookingResponse(Booking booking);
    List<BookingDetail> processBookingDetails(Booking booking, List<BookingDetailCreateRequest> detailRequests);
    CheckoutSummaryResponse getCheckoutSummary(Long bookingId);
    BookingResponse addServiceToExistingBooking(Long bookingId, List<BookingServiceRequest> serviceRequests);
}
