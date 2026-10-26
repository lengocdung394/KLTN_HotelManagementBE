package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutRequests;

import java.util.List;

@Service
public class PayOSPayoutGateway {
    private final PayOS payOS;

    public PayOSPayoutGateway(
            @Value("${CLIENT_ID}") String clientId,
            @Value("${API_KEY}") String apiKey,
            @Value("${CHECKSUM_KEY}") String checksumKey) {
        this.payOS = new PayOS(clientId, apiKey, checksumKey);
    }

    public Payout create(
            String referenceId,
            long amount,
            String description,
            String bankBin,
            String accountNumber) {
        PayoutRequests payoutRequest = PayoutRequests.builder()
                .referenceId(referenceId)
                .amount(amount)
                .description(description)
                .toBin(bankBin)
                .toAccountNumber(accountNumber)
                .category(List.of("booking-refund"))
                .build();
        return payOS.payouts().create(payoutRequest, referenceId);
    }

    public Payout get(String payoutId) {
        return payOS.payouts().get(payoutId);
    }
}
