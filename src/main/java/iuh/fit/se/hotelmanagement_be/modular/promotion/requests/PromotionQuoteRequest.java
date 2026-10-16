package iuh.fit.se.hotelmanagement_be.modular.promotion.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PromotionQuoteRequest(
        @NotNull @DecimalMin("0.00") BigDecimal roomTotal,
        @NotNull @DecimalMin("0.00") BigDecimal serviceTotal
) {
}
