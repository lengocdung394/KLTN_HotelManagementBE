package iuh.fit.se.hotelmanagement_be.modular.promotion.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.CustomerPromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.ClaimPromotionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerPromotionServiceImplTest {
    private PromotionRepository promotionRepository;
    private CustomerPromotionRepository customerPromotionRepository;
    private CustomerPromotionServiceImpl service;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        customerPromotionRepository = mock(CustomerPromotionRepository.class);
        service = new CustomerPromotionServiceImpl(promotionRepository, customerPromotionRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void onlyOneCustomerCanSaveRemainingSlotAfterOneDirectUse() {
        LocalDateTime now = LocalDateTime.now();
        Promotion promotion = Promotion.builder()
                .id("PROMO-1")
                .code("SUMMER")
                .status(PromotionStatus.ACTIVE)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(1))
                .usageLimit(2)
                .usedCount(1)
                .claimedCount(0)
                .build();
        ClaimPromotionRequest request = new ClaimPromotionRequest();
        request.setPromotionId(promotion.getId());

        when(promotionRepository.findByIdAndDeletedFalseForUpdate(promotion.getId()))
                .thenReturn(Optional.of(promotion));
        when(customerPromotionRepository.existsByCustomerIdAndPromotionId(any(), eq(promotion.getId())))
                .thenReturn(false);
        when(customerPromotionRepository.countByPromotionId(promotion.getId()))
                .thenReturn(0L, 1L);
        when(customerPromotionRepository.countUsedByPromotionId(promotion.getId()))
                .thenReturn(0L);

        setAuthenticatedCustomer("CUS-1");
        service.claimPromotion(request);
        assertEquals(1, promotion.getClaimedCount());

        setAuthenticatedCustomer("CUS-2");
        assertThrows(AppException.class, () -> service.claimPromotion(request));
    }

    private static void setAuthenticatedCustomer(String customerId) {
        Account account = mock(Account.class);
        when(account.getCustomer()).thenReturn(Customer.builder().id(customerId).build());

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(account);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
