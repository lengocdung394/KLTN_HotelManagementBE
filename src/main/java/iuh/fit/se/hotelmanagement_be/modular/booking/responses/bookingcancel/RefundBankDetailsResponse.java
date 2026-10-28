package iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel;

import lombok.Builder;

@Builder
public record RefundBankDetailsResponse(
        String bankName,
        String bankCode,
        String bankAccountName,
        String bankAccountNumber
) {
}
