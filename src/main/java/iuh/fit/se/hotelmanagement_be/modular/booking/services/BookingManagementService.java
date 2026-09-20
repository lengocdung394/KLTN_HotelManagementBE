package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.*;

import java.math.BigDecimal;
import java.util.List;

public interface BookingManagementService {
    BigDecimal processRoomDateUpdates(Booking booking, List<RoomDateUpdateRequest> dateUpdates);
    BookingModificationRequest modifyBooking(Long bookingId, BookingModificationRequest request);
    BigDecimal processRoomChanges(Booking booking, List<UpdateRoomChangeRequest> roomChanges);
    BigDecimal processServiceCancellations(Booking booking, List<ServiceCancellationRequest> cancellations);
    BigDecimal processRoomAdditions(Booking booking, List<NewRoomRequest> roomsToAdd);
    // Hủy phòng đi kèm với hủy dịch vụ của phòng đó
    BigDecimal processCancellations(Booking booking, List<Long> detailIdsToCancel, String operatorName);

}
