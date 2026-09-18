package iuh.fit.se.hotelmanagement_be.modular.booking.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class SurchargeCalculator {
    /**
     * Tính tiền phụ thu check-in sớm (tính trên ngày check-in đầu tiên)
     *
     * @param scheduledCheckInDate Ngày giờ nhận phòng dự kiến / quy chuẩn (ví dụ: 16/08/2026 14:00)
     * @param actualCheckInTime    Thời gian khách thực tế nhận phòng
     * @param nightlyRate          Giá phòng của đêm đó (1 đêm)
     */
    public static BigDecimal calculateEarlyCheckInFee(LocalDateTime scheduledCheckInDate, LocalDateTime actualCheckInTime, BigDecimal nightlyRate) {
        // Nếu khách nhận phòng đúng giờ hoặc trễ hơn giờ chuẩn thì không mất phí sớm
        if (actualCheckInTime == null || !actualCheckInTime.isBefore(scheduledCheckInDate)) {
            return BigDecimal.ZERO;
        }

        // Chỉ xét khoảng thời gian sớm trong cùng ngày check-in
        LocalTime actualTime = actualCheckInTime.toLocalTime();
        LocalTime morning06 = LocalTime.of(6, 0);
        LocalTime morning09 = LocalTime.of(9, 0);
        LocalTime standard14 = LocalTime.of(14, 0); // Giờ chuẩn 14:00

        if (actualTime.isBefore(morning06)) {
            // Nhận phòng trước 06:00 sáng: Phụ thu 100%
            return nightlyRate;
        } else if (!actualTime.isAfter(morning09)) {
            // Nhận phòng từ 06:00 – 09:00 sáng: Phụ thu 50%
            return nightlyRate.multiply(BigDecimal.valueOf(0.5));
        } else if (actualTime.isBefore(standard14)) {
            // Nhận phòng từ 09:00 – 14:00: Phụ thu 30%
            return nightlyRate.multiply(BigDecimal.valueOf(0.3));
        }

        return BigDecimal.ZERO;
    }

    /**
     * Tính tiền phụ thu check-out trễ (tính trên ngày check-out cuối cùng)
     *
     * @param scheduledCheckOutDate Ngày giờ trả phòng dự kiến / quy chuẩn (ví dụ: 20/08/2026 12:00)
     * @param actualCheckOutTime    Thời gian khách thực tế trả phòng
     * @param nightlyRate           Giá phòng của đêm đó (1 đêm)
     */
    public static BigDecimal calculateLateCheckOutFee(LocalDateTime scheduledCheckOutDate, LocalDateTime actualCheckOutTime, BigDecimal nightlyRate) {
        // Nếu khách trả phòng trước hoặc đúng giờ chuẩn thì không mất phí trễ
        if (actualCheckOutTime == null || !actualCheckOutTime.isAfter(scheduledCheckOutDate)) {
            return BigDecimal.ZERO;
        }

        LocalTime actualTime = actualCheckOutTime.toLocalTime();
        LocalTime afternoon15 = LocalTime.of(15, 0);
        LocalTime late18 = LocalTime.of(18, 0);

        if (!actualTime.isAfter(afternoon15)) {
            // Trả phòng từ 12:00 – 15:00: Phụ thu 30%
            return nightlyRate.multiply(BigDecimal.valueOf(0.3));
        } else if (!actualTime.isAfter(late18)) {
            // Trả phòng từ 15:00 – 18:00: Phụ thu 50%
            return nightlyRate.multiply(BigDecimal.valueOf(0.5));
        } else {
            // Trả phòng sau 18:00: Phụ thu 100%
            return nightlyRate.multiply(BigDecimal.valueOf(1.0));
        }
    }
}
