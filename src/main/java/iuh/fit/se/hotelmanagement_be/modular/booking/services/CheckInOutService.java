package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;

import java.util.List;

public interface CheckInOutService {
    // lây danh sach check in checkout
    List<BookingDetailResponse> getTodayCheckInList(Long hotelId);

    List<BookingDetailResponse> getTodayCheckOutList(Long hotelId);

    List<BookingDetailResponse> mapToBookingDetailResponseList(List<BookingDetail> details);

    // ham checkin checkout

    BookingResponse processBulkCheckIn(Long bookingId, List<Long> bookingDetailIds, Long employeeId);
    BookingResponse processBulkCheckOut(Long bookingId, List<Long> bookingDetailIds, Long employeeId);
}
