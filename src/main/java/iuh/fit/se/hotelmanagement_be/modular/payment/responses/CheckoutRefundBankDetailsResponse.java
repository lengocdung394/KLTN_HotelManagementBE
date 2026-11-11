package iuh.fit.se.hotelmanagement_be.modular.payment.responses;

import lombok.Builder;

@Builder
public record CheckoutRefundBankDetailsResponse(
        String bankName,
        String bankCode,
        String bankAccountName,
        String bankAccountNumber
) {
}
