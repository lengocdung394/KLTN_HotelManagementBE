package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponseForHotel;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.CheckoutSummaryResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.RoomMatrixResponse;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    // Tao booking cho khách hang
    BookingResponse createCustomerBooking(BookingCreateRequest request);

    // Nhân viên tạo booking tại quầy
    BookingResponse createCounterBooking(String employeeId, Long hotelId, BookingCreateRequest request);

    // Hàm chuyển đổi dữ liệu
    BookingResponse toBookingResponse(Booking booking);

    // Tiến hành tạo chi tiết của booking
    List<BookingDetail> processBookingDetails(Booking booking, List<BookingDetailCreateRequest> detailRequests);

    CheckoutSummaryResponse getCheckoutSummary(String bookingId);

    // Hàm chuyen đổi và Lay danh sach booking cua khách sạn
    BookingResponseForHotel toBookingForHotelResponse(Booking booking);

    List<BookingResponseForHotel> getBookingsByHotel(Long hotelId);

    List<RoomMatrixResponse> getRoomMatrix(Long hotelId, LocalDate startDate, LocalDate endDate);

}
