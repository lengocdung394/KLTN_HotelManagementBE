package iuh.fit.se.hotelmanagement_be.modular.auth.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoyaltyTier {

    BRONZE("Hạng Đồng", 0.0, 0.0),            // Chi tiêu < 5tr: Không giảm
    SILVER("Hạng Bạc", 5_000_000.0, 5.0),      // Chi tiêu >= 5tr: Giảm 5%
    GOLD("Hạng Vàng", 15_000_000.0, 10.0),     // Chi tiêu >= 15tr: Giảm 10%
    PLATINUM("Hạng Bạch Kim", 40_000_000.0, 15.0); // Chi tiêu >= 40tr: Giảm 15%

    private final String description;
    private final double minSpent;       // Số tiền tối thiểu để đạt hạng
    private final double discountPercent; // % Giảm giá mặc định của hạng

    // Hàm tiện ích: Tự động tìm Hạng dựa trên Tổng chi tiêu
    public static LoyaltyTier fromTotalSpent(double totalSpent) {
        if (totalSpent >= PLATINUM.minSpent) return PLATINUM;
        if (totalSpent >= GOLD.minSpent) return GOLD;
        if (totalSpent >= SILVER.minSpent) return SILVER;
        return BRONZE;
    }
}
