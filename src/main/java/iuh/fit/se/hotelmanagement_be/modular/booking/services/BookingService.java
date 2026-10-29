package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.*;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {
    // Tao booking cho khaÌch hang
    BookingResponse createCustomerBooking(BookingCreateRequest request);

    // NhÃ¢n viÃªn taÌ£o booking taÌ£i quÃ¢Ì€y
    BookingResponse createCounterBooking(String employeeId, Long hotelId, BookingCreateRequest request);

    // HaÌ€m chuyÃªÌ‰n Ä‘Ã´Ì‰i dÆ°Ìƒ liÃªÌ£u
    BookingResponse toBookingResponse(Booking booking);

    // TiÃªÌn haÌ€nh taÌ£o chi tiÃªÌt cuÌ‰a booking
    List<BookingDetail> processBookingDetails(Booking booking, List<BookingDetailCreateRequest> detailRequests);

    CheckoutSummaryResponse getCheckoutSummary(String bookingId);

    // HaÌ€m chuyen Ä‘Ã´Ì‰i vaÌ€ Lay danh sach booking cua khaÌch saÌ£n
    BookingResponseForHotel toBookingForHotelResponse(Booking booking);

    List<BookingResponseForHotel> getBookingsByHotel(Long hotelId);

    List<RoomMatrixResponse> getRoomMatrix(Long hotelId, LocalDate startDate, LocalDate endDate);

    void validatePromotionRules(Promotion promotion, BigDecimal currentTotalAmount, LocalDateTime now, BigDecimal roomTotal, BigDecimal serviceTotal);

    PromotionDiscountResult calculateDiscountBreakdown(Promotion promotion, BigDecimal roomTotal, BigDecimal serviceTotal);

    BigDecimal calculateBaseDiscount(BigDecimal baseAmount, Promotion promotion);

    List<BookingServiceDetail> processAndAttachServices(BookingDetail bookingDetail, List<BookingServiceRequest> serviceRequests);

    BigDecimal calculateTotalRoomPrice(List<BookingDetail> details);

    BigDecimal calculateTotalServicePrice(List<BookingDetail> bookingDetails);

    List<BookingResponse> getMyBookings(iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account account);
}