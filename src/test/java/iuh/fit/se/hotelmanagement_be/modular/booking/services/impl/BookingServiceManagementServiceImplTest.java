package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceUsage;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingDetailRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingManagementRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingServiceUsageRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.NewServiceRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.RoomServiceAdditionRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.ServiceCancellationRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.ServiceQuantityItem;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.UpdateServiceQuantityRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.CustomerPromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.services.PromotionUsageService;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomSeasonalRateRepository;
import iuh.fit.se.hotelmanagement_be.modular.service.entities.Service;
import iuh.fit.se.hotelmanagement_be.modular.service.repositories.ServiceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceManagementServiceImplTest {
    @Mock ServiceRepository serviceRepository;
    @Mock RoomRepository roomRepository;
    @Mock BookingManagementRepository bookingManagementRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingServiceUsageRepository bookingServiceUsageRepository;
    @Mock BookingDetailRepository bookingDetailRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock BranchRoomPolicyRepository branchRoomPolicyRepository;
    @Mock RoomSeasonalRateRepository roomSeasonalRateRepository;
    @Mock RoomPricingCalculator roomPricingCalculator;
    @Mock BookingServiceImpl bookingServiceImpl;
    @Mock PromotionRepository promotionRepository;
    @Mock CustomerPromotionRepository customerPromotionRepository;
    @Mock PromotionUsageService promotionUsageService;
    @Mock BookingSocketEmitter bookingSocketEmitter;
    @Mock EntityManager entityManager;

    @InjectMocks
    BookingServiceManagementServiceImpl bookingService;

    @Test
    void recordsEachPartialServiceConfirmationAsASeparateUsage() {
        Service catalogService = Service.builder()
                .id("SERVICE-1")
                .name("Breakfast")
                .price(50.0)
                .build();
        BookingServiceDetail serviceDetail = BookingServiceDetail.builder()
                .id("SERVICE-DETAIL-1")
                .service(catalogService)
                .quantity(3)
                .usedQuantity(0)
                .used(false)
                .price(50.0)
                .build();
        BookingDetail detail = bookingDetail(
                BookingStatusType.CHECKED_IN,
                new ArrayList<>(List.of(serviceDetail)));
        Booking booking = booking(detail);
        booking.setHotel(Hotel.builder().id(7L).build());
        Account account = mock(Account.class);
        when(account.getHotelId()).thenReturn(7L);
        when(account.getEmail()).thenReturn("staff@example.com");
        when(bookingRepository.findByIdForUpdate("BOOKING-1")).thenReturn(Optional.of(booking));
        when(bookingServiceUsageRepository.existsByBookingServiceDetail_Id(serviceDetail.getId()))
                .thenReturn(false, true);

        bookingService.confirmServiceUsed("BOOKING-1", serviceDetail.getId(), 1, account);
        bookingService.confirmServiceUsed("BOOKING-1", serviceDetail.getId(), 2, account);

        ArgumentCaptor<BookingServiceUsage> usageCaptor = ArgumentCaptor.forClass(BookingServiceUsage.class);
        verify(bookingServiceUsageRepository, org.mockito.Mockito.times(2)).save(usageCaptor.capture());
        assertEquals(List.of(1, 2), usageCaptor.getAllValues().stream()
                .map(BookingServiceUsage::getQuantity)
                .toList());
        assertEquals(List.of(50.0, 50.0), usageCaptor.getAllValues().stream()
                .map(BookingServiceUsage::getUnitPrice)
                .toList());
        assertEquals(3, serviceDetail.getUsedQuantity());
    }

    @Test
    void allowsAddingServicesToCheckedInRooms() {
        Service catalogService = Service.builder()
                .id("SERVICE-1")
                .name("Breakfast")
                .price(50.0)
                .build();
        BookingDetail detail = bookingDetail(BookingStatusType.CHECKED_IN, new ArrayList<>());
        Booking booking = booking(detail);
        when(serviceRepository.findById("SERVICE-1")).thenReturn(Optional.of(catalogService));

        BigDecimal added = bookingService.processServicesForExistingRooms(booking, List.of(
                RoomServiceAdditionRequest.builder()
                        .bookingDetailId(detail.getId())
                        .services(List.of(NewServiceRequest.builder()
                                .serviceId("SERVICE-1")
                                .quantity(2)
                                .build()))
                        .build()));

        assertEquals(new BigDecimal("100.0"), added);
        assertEquals(2, detail.getBookingServiceDetails().get(0).getQuantity());
        assertEquals(200.0, detail.getTotalPrice());
    }

    @Test
    void keepsDifferentPriceSnapshotsOnSeparateServiceLines() {
        Service catalogService = Service.builder()
                .id("SERVICE-1")
                .name("Breakfast")
                .price(200000.0)
                .build();
        BookingServiceDetail originalService = BookingServiceDetail.builder()
                .id("ORIGINAL-SERVICE")
                .service(catalogService)
                .quantity(3)
                .price(250000.0)
                .usedQuantity(0)
                .used(false)
                .isPaid(false)
                .build();
        BookingDetail detail = bookingDetail(
                BookingStatusType.CHECKED_IN,
                new ArrayList<>(List.of(originalService)));
        Booking booking = booking(detail);
        when(serviceRepository.findById("SERVICE-1")).thenReturn(Optional.of(catalogService));

        BigDecimal added = bookingService.processServicesForExistingRooms(booking, List.of(
                RoomServiceAdditionRequest.builder()
                        .bookingDetailId(detail.getId())
                        .services(List.of(NewServiceRequest.builder()
                                .serviceId("SERVICE-1")
                                .quantity(1)
                                .price(250000.0)
                                .build()))
                        .build()));

        assertEquals(new BigDecimal("200000.0"), added);
        assertEquals(2, detail.getBookingServiceDetails().size());
        assertEquals(3, originalService.getQuantity());
        assertEquals(250000.0, originalService.getPrice());
        BookingServiceDetail newlyAddedService = detail.getBookingServiceDetails().get(1);
        assertEquals(1, newlyAddedService.getQuantity());
        assertEquals(200000.0, newlyAddedService.getPrice());
        assertEquals(950000.0, detail.getServiceSubTotal());
        assertEquals(950100.0, detail.getTotalPrice());
    }

    @Test
    void allowsUpdatingServiceQuantityForCheckedInRooms() {
        Service catalogService = Service.builder()
                .id("SERVICE-1")
                .name("Breakfast")
                .price(50.0)
                .build();
        BookingServiceDetail serviceDetail = BookingServiceDetail.builder()
                .id("SERVICE-DETAIL-1")
                .service(catalogService)
                .quantity(3)
                .price(50.0)
                .build();
        BookingDetail detail = bookingDetail(BookingStatusType.CHECKED_IN, new ArrayList<>(List.of(serviceDetail)));
        Booking booking = booking(detail);

        BigDecimal change = bookingService.processServiceQuantityUpdates(booking, List.of(
                UpdateServiceQuantityRequest.builder()
                        .bookingDetailId(detail.getId())
                        .services(List.of(ServiceQuantityItem.builder()
                                .serviceId("SERVICE-1")
                                .quantity(2)
                                .build()))
                        .build()));

        assertEquals(new BigDecimal("-50.0"), change);
        assertEquals(2, serviceDetail.getQuantity());
        assertEquals(200.0, detail.getTotalPrice());
    }

    @Test
    void allowsCancellingUnusedDuplicateServiceWhenAnotherInstanceWasUsed() {
        Service catalogService = Service.builder()
                .id("SERVICE-1")
                .name("Breakfast")
                .price(50.0)
                .build();
        BookingServiceDetail usedService = BookingServiceDetail.builder()
                .id("USED-SERVICE-DETAIL")
                .service(catalogService)
                .quantity(1)
                .usedQuantity(1)
                .used(true)
                .price(50.0)
                .build();
        BookingServiceDetail unusedService = BookingServiceDetail.builder()
                .id("UNUSED-SERVICE-DETAIL")
                .service(catalogService)
                .quantity(1)
                .usedQuantity(0)
                .used(false)
                .isPaid(false)
                .price(50.0)
                .build();
        BookingDetail detail = bookingDetail(
                BookingStatusType.CHECKED_IN,
                new ArrayList<>(List.of(usedService, unusedService)));
        Booking booking = booking(detail);

        BigDecimal change = bookingService.processServiceCancellations(booking, List.of(
                ServiceCancellationRequest.builder()
                        .bookingDetailId(detail.getId())
                        .serviceDetailIds(List.of(unusedService.getId()))
                        .build()));

        assertEquals(new BigDecimal("-50.0"), change);
        assertEquals(false, usedService.getCancelled());
        assertEquals(true, unusedService.getCancelled());
    }

    @Test
    void allowsCancellingOnlyUnusedQuantityFromPartiallyUsedPaidService() {
        Service catalogService = Service.builder()
                .id("SERVICE-1")
                .name("Breakfast")
                .price(50.0)
                .build();
        BookingServiceDetail serviceDetail = BookingServiceDetail.builder()
                .id("PARTIALLY-USED-SERVICE")
                .service(catalogService)
                .quantity(2)
                .usedQuantity(1)
                .used(false)
                .isPaid(true)
                .price(50.0)
                .build();
        BookingDetail detail = bookingDetail(
                BookingStatusType.CHECKED_IN,
                new ArrayList<>(List.of(serviceDetail)));
        Booking booking = booking(detail);

        BigDecimal change = bookingService.processServiceCancellations(booking, List.of(
                ServiceCancellationRequest.builder()
                        .bookingDetailId(detail.getId())
                        .serviceDetailIds(List.of(serviceDetail.getId()))
                        .build()));

        assertEquals(new BigDecimal("-50.0"), change);
        assertEquals(1, serviceDetail.getQuantity());
        assertEquals(1, serviceDetail.getUsedQuantity());
        assertEquals(true, serviceDetail.getUsed());
        assertEquals(false, serviceDetail.getCancelled());
    }

    @Test
    void reducesAggregateQuantityByCancellingUnusedDuplicateServiceLine() {
        Service catalogService = Service.builder()
                .id("SERVICE-1")
                .name("Breakfast")
                .price(50.0)
                .build();
        BookingServiceDetail usedService = BookingServiceDetail.builder()
                .id("USED-SERVICE-DETAIL")
                .service(catalogService)
                .quantity(1)
                .usedQuantity(1)
                .used(true)
                .price(50.0)
                .build();
        BookingServiceDetail unusedService = BookingServiceDetail.builder()
                .id("UNUSED-SERVICE-DETAIL")
                .service(catalogService)
                .quantity(1)
                .usedQuantity(0)
                .used(false)
                .isPaid(false)
                .price(50.0)
                .build();
        BookingDetail detail = bookingDetail(
                BookingStatusType.CHECKED_IN,
                new ArrayList<>(List.of(usedService, unusedService)));
        Booking booking = booking(detail);

        BigDecimal change = bookingService.processServiceQuantityUpdates(booking, List.of(
                UpdateServiceQuantityRequest.builder()
                        .bookingDetailId(detail.getId())
                        .services(List.of(ServiceQuantityItem.builder()
                                .serviceId("SERVICE-1")
                                .quantity(1)
                                .build()))
                        .build()));

        assertEquals(new BigDecimal("-50.0"), change);
        assertEquals(1, usedService.getQuantity());
        assertEquals(false, usedService.getCancelled());
        assertEquals(true, unusedService.getCancelled());
        assertEquals(150.0, detail.getTotalPrice());
    }

    @Test
    void stillRejectsAddingServicesToCancelledRooms() {
        BookingDetail detail = bookingDetail(BookingStatusType.CANCELLED, new ArrayList<>());
        Booking booking = booking(detail);

        assertThrows(AppException.class, () -> bookingService.processServicesForExistingRooms(
                booking,
                List.of(RoomServiceAdditionRequest.builder()
                        .bookingDetailId(detail.getId())
                        .services(List.of(NewServiceRequest.builder()
                                .serviceId("SERVICE-1")
                                .quantity(1)
                                .build()))
                        .build())));
    }

    private BookingDetail bookingDetail(BookingStatusType status, List<BookingServiceDetail> services) {
        return BookingDetail.builder()
                .id("DETAIL-1")
                .status(status)
                .roomSubTotal(100.0)
                .bookingServiceDetails(services)
                .build();
    }

    private Booking booking(BookingDetail detail) {
        return Booking.builder()
                .id("BOOKING-1")
                .bookingDetails(new ArrayList<>(List.of(detail)))
                .build();
    }
}
