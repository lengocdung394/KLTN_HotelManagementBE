package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Function;

public class SurchargeCalculator {

    public static BigDecimal calculateEarlyCheckInFee(LocalDateTime scheduledCheckIn,
                                                      LocalDateTime actualCheckIn,
                                                      Function<LocalDate, BigDecimal> dailyRateProvider) {
        if (scheduledCheckIn == null || actualCheckIn == null || dailyRateProvider == null) {
            return BigDecimal.ZERO;
        }

        if (!actualCheckIn.isBefore(scheduledCheckIn)) {
            return BigDecimal.ZERO;
        }

        BigDecimal fee = BigDecimal.ZERO;
        LocalDateTime currentPointer = actualCheckIn;
        LocalDate scheduledDate = scheduledCheckIn.toLocalDate();

        while (currentPointer.isBefore(scheduledCheckIn)) {
            LocalDate currentDate = currentPointer.toLocalDate();
            BigDecimal dailyRate = dailyRateProvider.apply(currentDate); // Lấy đúng giá phòng của ngày hiện tại

            LocalDateTime endOfDay = currentDate.plusDays(1).atStartOfDay(); // 00:00 ngày hôm sau
            LocalDateTime targetLimit = endOfDay.isBefore(scheduledCheckIn) ? endOfDay : scheduledCheckIn;

            long minutesInThisDay = Duration.between(currentPointer, targetLimit).toMinutes();

            if (currentDate.equals(scheduledDate)) {
                // Đây là ngày diễn ra lịch check-in dự kiến (ví dụ ngày 10/09)
                // Áp dụng đúng các mốc quy định giờ check-in sớm:
                LocalTime actualTime = currentPointer.toLocalTime();
                BigDecimal partialFee = BigDecimal.ZERO;

                if (actualTime.isBefore(LocalTime.of(6, 0))) {
                    partialFee = dailyRate; // Trước 06:00 sáng: 100% giá 1 đêm
                } else if (!actualTime.isAfter(LocalTime.of(9, 0))) {
                    partialFee = dailyRate.multiply(BigDecimal.valueOf(0.5)); // Từ 06:00 – 09:00: 50%
                } else {
                    partialFee = dailyRate.multiply(BigDecimal.valueOf(0.3)); // Từ 09:00 – 14:00: 30%
                }
                fee = fee.add(partialFee);
            } else {
                // Các ngày trước đó (như ngày 09/09 trong ví dụ của bạn)
                if (minutesInThisDay >= 1440) {
                    // Đủ nguyên 1 ngày -> Tính 100% giá của ngày đó
                    fee = fee.add(dailyRate);
                } else {
                    // Phần giờ lẻ của những ngày trước đó (tính tỷ lệ thuận theo số phút)
                    BigDecimal partialRate = dailyRate
                            .multiply(BigDecimal.valueOf(minutesInThisDay))
                            .divide(BigDecimal.valueOf(24L * 60L), 6, RoundingMode.HALF_UP);
                    fee = fee.add(partialRate);
                }
            }

            currentPointer = targetLimit;
        }

        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateLateCheckOutFee(LocalDateTime scheduledCheckOut,
                                                      LocalDateTime actualCheckOut,
                                                      Function<LocalDate, BigDecimal> dailyRateProvider) {
        if (scheduledCheckOut == null || actualCheckOut == null || dailyRateProvider == null) {
            return BigDecimal.ZERO;
        }

        if (!actualCheckOut.isAfter(scheduledCheckOut)) {
            return BigDecimal.ZERO;
        }

        BigDecimal fee = BigDecimal.ZERO;
        LocalDateTime currentPointer = scheduledCheckOut;
        LocalDate scheduledDate = scheduledCheckOut.toLocalDate();

        while (currentPointer.isBefore(actualCheckOut)) {
            LocalDate currentDate = currentPointer.toLocalDate();
            BigDecimal dailyRate = dailyRateProvider.apply(currentDate);

            LocalDateTime nextDay = currentDate.plusDays(1).atStartOfDay();
            LocalDateTime targetLimit = nextDay.isBefore(actualCheckOut) ? nextDay : actualCheckOut;

            long minutesInThisDay = Duration.between(currentPointer, targetLimit).toMinutes();

            if (currentDate.equals(scheduledDate)) {
                // Ngày checkout dự kiến, áp dụng mốc giờ trễ
                LocalTime actualTime = actualCheckOut.toLocalTime();
                BigDecimal partialFee = BigDecimal.ZERO;

                if (actualTime.isBefore(LocalTime.of(15, 0))) {
                    partialFee = dailyRate.multiply(BigDecimal.valueOf(0.3)); // 12:00 – 15:00: 30%
                } else if (actualTime.isBefore(LocalTime.of(18, 0))) {
                    partialFee = dailyRate.multiply(BigDecimal.valueOf(0.5)); // 15:00 – 18:00: 50%
                } else {
                    partialFee = dailyRate; // Sau 18:00: 100%
                }
                fee = fee.add(partialFee);
            } else {
                if (minutesInThisDay >= 1440) {
                    fee = fee.add(dailyRate);
                } else {
                    BigDecimal partialRate = dailyRate
                            .multiply(BigDecimal.valueOf(minutesInThisDay))
                            .divide(BigDecimal.valueOf(24L * 60L), 6, RoundingMode.HALF_UP);
                    fee = fee.add(partialRate);
                }
            }

            currentPointer = targetLimit;
        }

        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}