package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class SurchargeCalculator {

    public static BigDecimal calculateEarlyCheckInFee(LocalDateTime scheduledCheckIn,
                                                      LocalDateTime actualCheckIn,
                                                      BigDecimal nightlyRate) {
        if (scheduledCheckIn == null || actualCheckIn == null || nightlyRate == null) {
            return BigDecimal.ZERO;
        }

        if (!actualCheckIn.isBefore(scheduledCheckIn)) {
            return BigDecimal.ZERO;
        }

        long totalMinutesEarly = Duration.between(actualCheckIn, scheduledCheckIn).toMinutes();

        long fullDays = totalMinutesEarly / (24L * 60L);
        long remainingMinutes = totalMinutesEarly % (24L * 60L);

        BigDecimal fee = BigDecimal.ZERO;

        // Phần đúng số ngày
        if (fullDays > 0) {
            fee = fee.add(nightlyRate.multiply(BigDecimal.valueOf(fullDays)));
        }

        // Phần giờ còn lại trong ngày
        if (remainingMinutes > 0) {
            BigDecimal partialRate = nightlyRate
                    .multiply(BigDecimal.valueOf(remainingMinutes))
                    .divide(BigDecimal.valueOf(24L * 60L), 6, RoundingMode.HALF_UP);

            fee = fee.add(partialRate);
        }

        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateLateCheckOutFee(LocalDateTime scheduledCheckOut,
                                                      LocalDateTime actualCheckOut,
                                                      BigDecimal nightlyRate) {
        if (scheduledCheckOut == null || actualCheckOut == null || nightlyRate == null) {
            return BigDecimal.ZERO;
        }

        if (!actualCheckOut.isAfter(scheduledCheckOut)) {
            return BigDecimal.ZERO;
        }

        long totalMinutesLate = Duration.between(scheduledCheckOut, actualCheckOut).toMinutes();

        long fullDays = totalMinutesLate / (24L * 60L);
        long remainingMinutes = totalMinutesLate % (24L * 60L);

        BigDecimal fee = BigDecimal.ZERO;

        if (fullDays > 0) {
            fee = fee.add(nightlyRate.multiply(BigDecimal.valueOf(fullDays)));
        }

        if (remainingMinutes > 0) {
            BigDecimal partialRate = nightlyRate
                    .multiply(BigDecimal.valueOf(remainingMinutes))
                    .divide(BigDecimal.valueOf(24L * 60L), 6, RoundingMode.HALF_UP);

            fee = fee.add(partialRate);
        }

        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}