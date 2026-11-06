package iuh.fit.se.hotelmanagement_be.modular.auth.services.impl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingPriceSummaryTest {

    @Test
    void calculatesPreDiscountSubtotalAndTotalSavings() {
        EmailService.BookingPriceSummary summary = new EmailService.BookingPriceSummary(
                new BigDecimal("5000"),
                new BigDecimal("200000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1000"),
                new BigDecimal("204000"));

        assertEquals(new BigDecimal("205000"), summary.subtotal());
        assertEquals(new BigDecimal("1000"), summary.totalSavings());
    }
}
