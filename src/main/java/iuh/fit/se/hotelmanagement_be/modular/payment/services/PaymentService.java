package iuh.fit.se.hotelmanagement_be.modular.payment.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.CashPaymentRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.PaymentRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.PaymentResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.PaymentTransactionResponse;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.WebhookResponse;

public interface PaymentService  {
    PaymentResponse createVietQRPaymentLink(PaymentRequest request) throws Exception;
    WebhookResponse processPayOSWebhook(JsonNode webhookBody) throws Exception;
    PaymentTransactionResponse payWithCash(CashPaymentRequest request);

}
