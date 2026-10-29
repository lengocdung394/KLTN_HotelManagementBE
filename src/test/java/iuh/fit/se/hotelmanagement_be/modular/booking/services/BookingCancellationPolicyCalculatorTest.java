package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingCancellationPolicyCalculatorTest {
    @Test
    void appliesExistingCancellationPenaltyThresholds() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 12, 0);

        assertEquals(
                BigDecimal.ZERO,
                BookingCancellationPolicyCalculator.calculate(now.plusHours(72), now).roomPenaltyRate());
        assertEquals(
                new BigDecimal("0.50"),
                BookingCancellationPolicyCalculator.calculate(now.plusHours(24), now).roomPenaltyRate());
        assertEquals(
                BigDecimal.ONE,
                BookingCancellationPolicyCalculator.calculate(now.plusHours(23), now).roomPenaltyRate());
    }

    @Test
    void cancellationAfterCheckInUsesFullPenaltyAndZeroRemainingHours() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 12, 0);

        BookingCancellationPolicyCalculator.Policy policy =
                BookingCancellationPolicyCalculator.calculate(now.minusHours(1), now);

        assertEquals(BigDecimal.ONE, policy.roomPenaltyRate());
        assertEquals(0, policy.hoursBeforeCheckIn());
    }
}
