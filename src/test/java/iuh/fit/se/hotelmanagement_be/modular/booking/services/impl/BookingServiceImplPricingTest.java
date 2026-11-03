package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmailService;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingCancellationRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingDetailRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.ExtraFeeBreakdownResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Building;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Floor;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.CustomerPromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.services.PromotionUsageService;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.RoomSeasonalRate;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomStatus;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.enums.RoomType;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomSeasonalRateRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplPricingTest {
    @Mock CustomerRepository customerRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock RoomRepository roomRepository;
    @Mock BranchRoomPolicyRepository branchRoomPolicyRepository;
    @Mock RoomPricingCalculator roomPricingCalculator;
    @Mock ServiceRepository serviceRepository;
    @Mock BookingRepository bookingRepository;
    @Mock CustomerPromotionRepository customerPromotionRepository;
    @Mock PromotionRepository promotionRepository;
    @Mock HotelRepository hotelRepository;
    @Mock RoomSeasonalRateRepository roomSeasonalRateRepository;
    @Mock BookingDetailRepository bookingDetailRepository;
    @Mock BookingCancellationRepository bookingCancellationRepository;
    @Mock BookingSocketEmitter bookingSocketEmitter;
    @Mock PromotionUsageService promotionUsageService;
    @Mock EmailService emailService;
    @Mock AccountRepository accountRepository;
    @Mock PaymentRepository paymentRepository;

    @InjectMocks BookingServiceImpl bookingService;

    @Test
    void appliesSingleNightEventRateAndAddsRoomAmenities() {
        Long hotelId = 7L;
        String roomId = "ROOM-101";
        LocalDate eventDate = LocalDate.of(2026, 11, 10);
        Hotel hotel = org.mockito.Mockito.mock(Hotel.class);
        when(hotel.getId()).thenReturn(hotelId);
        Booking booking = Booking.builder().hotel(hotel).build();

        Room room = org.mockito.Mockito.mock(Room.class);
        Floor floor = org.mockito.Mockito.mock(Floor.class);
        Building building = org.mockito.Mockito.mock(Building.class);
        when(room.getId()).thenReturn(roomId);
        when(room.getRoomType()).thenReturn(RoomType.STANDARD);
        when(room.getRoomStatus()).thenReturn(RoomStatus.READY);
        when(room.getTotalAmenitiesPrice()).thenReturn(30000.0);
        when(room.getFloor()).thenReturn(floor);
        when(floor.getBuilding()).thenReturn(building);
        when(building.getHotel()).thenReturn(hotel);
        when(roomRepository.findByIdAndFloor_Building_Hotel_Id(roomId, hotelId)).thenReturn(Optional.of(room));
        when(roomRepository.findByIdWithLock(roomId)).thenReturn(Optional.of(room));
        BranchRoomPolicy policy = org.mockito.Mockito.mock(BranchRoomPolicy.class);
        when(policy.getExtraAdultFee()).thenReturn(0.0);
        when(policy.getExtraChildFee()).thenReturn(0.0);
        when(policy.getStandardCapacity()).thenReturn(2);
        when(policy.getMaxExtraGuests()).thenReturn(1);
        when(branchRoomPolicyRepository.findByHotelIdAndRoomType(hotelId, RoomType.STANDARD)).thenReturn(policy);
        when(roomPricingCalculator.calculateExtraFeeBreakdown(policy, 2, 0))
                .thenReturn(new ExtraFeeBreakdownResponse(0.0, 0.0, 0.0));

        RoomSeasonalRate eventRate = RoomSeasonalRate.builder()
                .hotelId(hotelId)
                .roomType(RoomType.STANDARD)
                .rateName("One-day event")
                .startDate(eventDate)
                .endDate(eventDate)
                .price(920000.0)
                .build();
        when(roomSeasonalRateRepository.findRatesByBranchAndDateRange(hotelId, eventDate, eventDate))
                .thenReturn(List.of(eventRate));

        BookingDetailCreateRequest detailRequest = BookingDetailCreateRequest.builder()
                .roomId(roomId)
                .checkInTime(eventDate.atTime(14, 0))
                .checkOutTime(eventDate.plusDays(1).atTime(12, 0))
                .numAdults(2)
                .numChildren(0)
                .numInfants(0)
                .build();

        List<BookingDetail> details = bookingService.processBookingDetails(booking, List.of(detailRequest));

        assertEquals(1, details.get(0).getNightlyPrices().size());
        assertEquals(
                0,
                new BigDecimal("950000.00").compareTo(details.get(0).getNightlyPrices().get(0).getNightTotal())
        );
    }
}
