package iuh.fit.se.hotelmanagement_be.modular.promotion.responses;

import java.math.BigDecimal;

public record PromotionQuoteResponse(
        boolean eligible,
        BigDecimal discountAmount,
        BigDecimal payableAmount,
        String reason
) {}
