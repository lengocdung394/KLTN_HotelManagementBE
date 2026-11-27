package iuh.fit.se.hotelmanagement_be.modular.promotion.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromotionUsageLimitTest {
    @Test
    void allowsOneSavedVoucherAfterOneDirectUseAtLimitTwo() {
        assertFalse(PromotionUsageLimit.isExhausted(2, 0, 1, 0));
    }

    @Test
    void blocksAnotherSavedVoucherAfterOneDirectUseAndOneSavedVoucher() {
        assertTrue(PromotionUsageLimit.isExhausted(2, 1, 1, 0));
    }

    @Test
    void doesNotCountUseOfSavedVoucherTwice() {
        assertFalse(PromotionUsageLimit.isExhausted(2, 1, 1, 1));
    }

    @Test
    void treatsNullLimitAsUnlimited() {
        assertFalse(PromotionUsageLimit.isExhausted(null, 5, 5, 2));
    }
}
