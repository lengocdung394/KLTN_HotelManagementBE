package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class BookingNightlyPriceResponse {
    LocalDate stayDate;
    BigDecimal roomBasePrice;
    BigDecimal extraAdultFee;
    BigDecimal extraChildFee;
    BigDecimal extraAdultUnitPrice;
    BigDecimal extraChildUnitPrice;
    Integer standardCapacity;
    Integer maxExtraGuests;
    BigDecimal nightTotal;
}
