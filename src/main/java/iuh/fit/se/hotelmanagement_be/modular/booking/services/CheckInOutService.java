package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingDetailForCheckInOutResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CheckInOutService {
    // lây danh sach check in checkout
    List<BookingDetailForCheckInOutResponse> getTodayCheckInList(Long hotelId, LocalDate date, BookingStatusType status, BookingStatus bookingStatus);

    List<BookingDetailForCheckInOutResponse> getTodayCheckOutList(Long hotelId, LocalDate date, BookingStatusType status, BookingStatus bookingStatus);


    List<BookingDetailForCheckInOutResponse> mapToBookingDetailResponseList(List<BookingDetail> details);

    // ham checkin checkout

    BookingResponse processBulkCheckIn(String bookingId, List<Long> bookingDetailIds, String employeeId);

    BookingResponse processBulkCheckOut(String bookingId, List<Long> bookingDetailIds, String employeeId);
}
