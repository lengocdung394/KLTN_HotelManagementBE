package iuh.fit.se.hotelmanagement_be.modular.report.responses;

import java.math.BigDecimal;

public record PromotionUsageReportResponse(
        String promotionId,
        String promotionName,
        String promotionCode,
        long usageCount,
        BigDecimal savings
) {
}
