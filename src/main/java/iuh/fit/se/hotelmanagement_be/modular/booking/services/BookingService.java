package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponseForHotel;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.CheckoutSummaryResponse;

import java.util.List;

public interface BookingService {
    // Tao booking cho khách hang
    BookingResponse createCustomerBooking(BookingCreateRequest request);

    // Nhân viên tạo booking tại quầy
    BookingResponse createCounterBooking(Long employeeId, BookingCreateRequest request);

    // Hàm chuyển đổi dữ liệu
    BookingResponse toBookingResponse(Booking booking);

    // Tiến hành tạo chi tiết của booking
    List<BookingDetail> processBookingDetails(Booking booking, List<BookingDetailCreateRequest> detailRequests);

    CheckoutSummaryResponse getCheckoutSummary(Long bookingId);

    BookingResponse addServiceToExistingBooking(Long bookingId, List<BookingServiceRequest> serviceRequests);

    BookingResponse cancelRooms(Long bookingId, List<Long> bookingDetailIds, Long employeeId);

    BookingResponse cancelEntireBooking(Long bookingId, Long employeeId);

    BookingResponse addServicesToRoom(Long bookingId, Long bookingDetailId, List<BookingServiceRequest> serviceRequests);

    BookingResponse addRoomToExistingBooking(Long bookingId, List<BookingDetailCreateRequest> additionalRoomRequests);

    BookingResponse updateOrCancelServices(Long bookingId, Long bookingDetailId, List<BookingServiceRequest> updatedServiceRequests, Long customerId);

    // Hàm chuyen đổi và Lay danh sach booking cua khách sạn
    BookingResponseForHotel toBookingForHotelResponse(Booking booking);

    List<BookingResponseForHotel> getBookingsByHotel(Long hotelId);



}
