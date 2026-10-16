package iuh.fit.se.hotelmanagement_be.modular.promotion.responses;

import java.math.BigDecimal;

public record PromotionQuoteResponse(
        boolean eligible,
        BigDecimal estimatedDiscountAmount,
        BigDecimal payableAmount,
        String reason
) {}
