package iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PayOSPayoutStateConverter implements AttributeConverter<PayOSPayoutState, String> {
    @Override
    public String convertToDatabaseColumn(PayOSPayoutState state) {
        return state == null ? null : state.name();
    }

    @Override
    public PayOSPayoutState convertToEntityAttribute(String value) {
        return PayOSPayoutState.fromValue(value);
    }
}
