package iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums;

import java.util.Locale;

public enum PayOSPayoutState {
    PROCESSING,
    TRANSFERRED,
    FAILED,
    UNKNOWN;

    public static PayOSPayoutState fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "DRAFTING", "SUBMITTED", "APPROVED", "SCHEDULED", "PROCESSING", "PARTIAL_COMPLETED" ->
                    PROCESSING;
            case "COMPLETED", "TRANSFERRED" -> TRANSFERRED;
            case "REJECTED", "CANCELLED", "FAILED" -> FAILED;
            default -> UNKNOWN;
        };
    }
}
