package iuh.fit.se.hotelmanagement_be.modular.promotion.services;

public final class PromotionUsageLimit {
    private PromotionUsageLimit() {
    }

    public static boolean isExhausted(
            Integer usageLimit,
            long savedVoucherCount,
            int usedCount,
            long usedSavedVoucherCount
    ) {
        if (usageLimit == null) {
            return false;
        }

        long directUseCount = Math.max(0L, (long) usedCount - usedSavedVoucherCount);
        return Math.max(0L, savedVoucherCount) + directUseCount >= usageLimit;
    }
}
