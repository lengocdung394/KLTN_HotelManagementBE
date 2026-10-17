package iuh.fit.se.hotelmanagement_be.modular.promotion.responses;

import java.math.BigDecimal;

public record PromotionSummaryResponse(
        long activePromotions,
        long monthlyUsageCount,
        BigDecimal monthlySavingsAmount,
        long expiringSoonPromotions
) {
}
