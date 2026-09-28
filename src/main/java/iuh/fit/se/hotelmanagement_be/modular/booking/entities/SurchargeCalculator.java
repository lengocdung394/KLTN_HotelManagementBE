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

        // Nếu thực tế check-in bằng hoặc sau giờ dự kiến thì không tính phụ thu sớm
        if (!actualCheckIn.isBefore(scheduledCheckIn)) {
            return BigDecimal.ZERO;
        }

        LocalDate scheduledDate = scheduledCheckIn.toLocalDate();
        LocalDate actualDate = actualCheckIn.toLocalDate();

        BigDecimal fee = BigDecimal.ZERO;

        // Trường hợp 1: Check-in sớm từ ngày hôm trước trở về trước (khác ngày)
        // Ví dụ: Lịch 16/8 mà đến từ ngày 15/8 (bất kể mấy giờ)
        if (actualDate.isBefore(scheduledDate)) {
            LocalDate pointerDate = actualDate;
            // Duyệt qua từng ngày từ ngày thực tế đến sát ngày dự kiến
            while (pointerDate.isBefore(scheduledDate)) {
                BigDecimal dailyRate = dailyRateProvider.apply(pointerDate);
                if (dailyRate != null) {
                    fee = fee.add(dailyRate); // Tính trọn vẹn 100% tiền phòng của mỗi ngày đến sớm
                }
                pointerDate = pointerDate.plusDays(1);
            }
            return fee.setScale(2, RoundingMode.HALF_UP);
        }

        // Trường hợp 2: Check-in sớm trong CÙNG MỘT NGÀY dự kiến (ví dụ cùng ngày 16/8 nhưng đến sớm)
        if (actualDate.equals(scheduledDate)) {
            LocalTime actualTime = actualCheckIn.toLocalTime();
            BigDecimal dailyRate = dailyRateProvider.apply(scheduledDate);
            if (dailyRate == null) return BigDecimal.ZERO;

            if (actualTime.isBefore(LocalTime.of(6, 0))) {
                return dailyRate; // Trước 06:00 sáng: 100% giá 1 đêm
            } else if (!actualTime.isAfter(LocalTime.of(9, 0))) {
                return dailyRate.multiply(BigDecimal.valueOf(0.5)); // Từ 06:00 – 09:00: 50%
            } else if (actualTime.isBefore(scheduledCheckIn.toLocalTime())) {
                return dailyRate.multiply(BigDecimal.valueOf(0.3)); // Từ 09:00 đến trước giờ chuẩn: 30%
            }
        }

        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateLateCheckOutFee(LocalDateTime scheduledCheckOut,
                                                      LocalDateTime actualCheckOut,
                                                      BigDecimal dailyRate) {
        if (scheduledCheckOut == null || actualCheckOut == null || dailyRate == null) {
            return BigDecimal.ZERO;
        }

        // Nếu trả trước hoặc đúng giờ chuẩn (12:00) thì không phụ thu
        if (!actualCheckOut.isAfter(scheduledCheckOut)) {
            return BigDecimal.ZERO;
        }

        LocalTime actualTime = actualCheckOut.toLocalTime();

        // 1. Trả phòng từ 12:30 đến 15:00 -> Phụ thu 30%
        if (!actualTime.isAfter(LocalTime.of(15, 0))) {
            return dailyRate.multiply(BigDecimal.valueOf(0.3));
        }

        // 2. Trả phòng từ 15:00 đến 18:00 -> Phụ thu 50%
        if (!actualTime.isAfter(LocalTime.of(18, 0))) {
            return dailyRate.multiply(BigDecimal.valueOf(0.5));
        }

        // 3. Trả phòng sau 18:00 -> Tính thêm 100% giá 1 đêm
        return dailyRate;
    }
}