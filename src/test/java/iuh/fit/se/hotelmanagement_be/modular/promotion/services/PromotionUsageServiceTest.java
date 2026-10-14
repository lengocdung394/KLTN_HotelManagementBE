package iuh.fit.se.hotelmanagement_be.modular.promotion.services;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.UserPromotionUsage;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.CustomerPromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.UserPromotionUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionUsageServiceTest {
    private PromotionRepository promotionRepository;
    private CustomerPromotionRepository customerPromotionRepository;
    private UserPromotionUsageRepository usageRepository;
    private PromotionUsageService service;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        customerPromotionRepository = mock(CustomerPromotionRepository.class);
        usageRepository = mock(UserPromotionUsageRepository.class);
        service = new PromotionUsageService(
                promotionRepository,
                customerPromotionRepository,
                usageRepository
        );
    }

    @Test
    void applyingPromotionReservesUsageImmediately() {
        Customer customer = Customer.builder().id("CUS-1").build();
        Promotion promotion = promotion("PROMO-1", 0);
        Booking booking = booking("BOOK-1", customer);

        when(promotionRepository.findByIdForUpdate(promotion.getId()))
                .thenReturn(Optional.of(promotion));
        when(usageRepository.findByCustomerAndPromotionCodeForUpdate(customer.getId(), promotion.getCode()))
                .thenReturn(Optional.empty());

        service.replaceBookingPromotion(booking, promotion, null);

        assertEquals(1, promotion.getUsedCount());
        verify(promotionRepository).save(promotion);
        verify(usageRepository).save(any(UserPromotionUsage.class));
    }

    @Test
    void anotherBookingCannotReserveTheSamePromotionForTheCustomer() {
        Customer customer = Customer.builder().id("CUS-1").build();
        Promotion promotion = promotion("PROMO-1", 1);
        Booking firstBooking = booking("BOOK-1", customer);
        Booking secondBooking = booking("BOOK-2", customer);
        UserPromotionUsage existingUsage = UserPromotionUsage.builder()
                .customer(customer)
                .promotionCode(promotion.getCode())
                .booking(firstBooking)
                .build();

        when(promotionRepository.findByIdForUpdate(promotion.getId()))
                .thenReturn(Optional.of(promotion));
        when(usageRepository.findByCustomerAndPromotionCodeForUpdate(customer.getId(), promotion.getCode()))
                .thenReturn(Optional.of(existingUsage));

        assertThrows(
                AppException.class,
                () -> service.replaceBookingPromotion(secondBooking, promotion, null)
        );
    }

    @Test
    void removingPromotionReturnsItsUsageToTheCustomer() {
        Customer customer = Customer.builder().id("CUS-1").build();
        Promotion promotion = promotion("PROMO-1", 1);
        Booking booking = booking("BOOK-1", customer);
        booking.setPromotion(promotion);
        UserPromotionUsage existingUsage = UserPromotionUsage.builder()
                .customer(customer)
                .promotionCode(promotion.getCode())
                .booking(booking)
                .build();

        when(promotionRepository.findByIdForUpdate(promotion.getId()))
                .thenReturn(Optional.of(promotion));
        when(usageRepository.findByCustomerAndBookingForUpdate(customer.getId(), booking.getId()))
                .thenReturn(Optional.of(existingUsage));

        service.replaceBookingPromotion(booking, null, null);

        assertEquals(0, promotion.getUsedCount());
        verify(usageRepository).delete(existingUsage);
        assertNull(booking.getPromotion());
    }

    @Test
    void removingCustomerVoucherMakesItAvailableAgain() {
        Customer customer = Customer.builder().id("CUS-1").build();
        Promotion promotion = promotion("PROMO-1", 1);
        Booking booking = booking("BOOK-1", customer);
        CustomerPromotion voucher = CustomerPromotion.builder()
                .id("CP-1")
                .customer(customer)
                .promotion(promotion)
                .booking(booking)
                .isUsed(true)
                .usedAt(java.time.LocalDateTime.now())
                .build();
        booking.setCustomerPromotion(voucher);
        UserPromotionUsage existingUsage = UserPromotionUsage.builder()
                .customer(customer)
                .promotionCode(promotion.getCode())
                .booking(booking)
                .build();

        when(promotionRepository.findByIdForUpdate(promotion.getId()))
                .thenReturn(Optional.of(promotion));
        when(usageRepository.findByCustomerAndBookingForUpdate(customer.getId(), booking.getId()))
                .thenReturn(Optional.of(existingUsage));
        when(customerPromotionRepository.findByIdAndCustomerIdForUpdate(voucher.getId(), customer.getId()))
                .thenReturn(Optional.of(voucher));

        service.replaceBookingPromotion(booking, null, null);

        assertFalse(voucher.isUsed());
        assertNull(voucher.getUsedAt());
        assertNull(voucher.getBooking());
        verify(customerPromotionRepository).save(voucher);
    }

    private static Promotion promotion(String id, int usedCount) {
        return Promotion.builder()
                .id(id)
                .code("SUMMER")
                .usedCount(usedCount)
                .build();
    }

    private static Booking booking(String id, Customer customer) {
        return Booking.builder()
                .id(id)
                .customer(customer)
                .build();
    }
}
