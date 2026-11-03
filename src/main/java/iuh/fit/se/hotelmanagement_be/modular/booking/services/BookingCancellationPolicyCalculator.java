package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public final class BookingCancellationPolicyCalculator {
    private BookingCancellationPolicyCalculator() {
    }

    public static Policy calculate(LocalDateTime checkIn, LocalDateTime now) {
        Duration timeUntilCheckIn = Duration.between(now, checkIn);
        long secondsBeforeCheckIn = timeUntilCheckIn.getSeconds();
        long hoursBeforeCheckIn = Math.max(0, timeUntilCheckIn.toHours());
        if (secondsBeforeCheckIn >= Duration.ofHours(72).getSeconds()) {
            return new Policy(BigDecimal.ZERO, "CANCEL_AT_LEAST_72_HOURS_BEFORE_CHECK_IN", hoursBeforeCheckIn);
        }
        if (secondsBeforeCheckIn >= Duration.ofHours(24).getSeconds()) {
            return new Policy(new BigDecimal("0.50"), "CANCEL_24_TO_72_HOURS_BEFORE_CHECK_IN", hoursBeforeCheckIn);
        }
        return new Policy(BigDecimal.ONE, "CANCEL_WITHIN_24_HOURS_OR_NO_SHOW", hoursBeforeCheckIn);
    }

    public record Policy(BigDecimal roomPenaltyRate, String name, long hoursBeforeCheckIn) {
    }
}
