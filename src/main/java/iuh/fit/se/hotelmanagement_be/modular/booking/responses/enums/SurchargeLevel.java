package iuh.fit.se.hotelmanagement_be.modular.booking.responses.enums;

public enum SurchargeLevel {
    NONE(0),          // Chưa phạt (trong giờ ân hạn)
    LEVEL_30_PERCENT(30), // Mức 30% (từ 12:30 đến 15:00)
    LEVEL_50_PERCENT(50), // Mức 50% (từ 15:00 đến 18:00)
    LEVEL_100_PERCENT(100);// Mức 100% (sau 18:00 hoặc qua ngày hôm sau)

    private final int percentage;

    SurchargeLevel(int percentage) {
        this.percentage = percentage;
    }

    public int getPercentage() {
        return percentage;
    }
}
