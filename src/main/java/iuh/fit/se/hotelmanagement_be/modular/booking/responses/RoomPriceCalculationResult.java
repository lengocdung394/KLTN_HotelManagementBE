package iuh.fit.se.hotelmanagement_be.modular.booking.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomPriceCalculationResult {
    private BigDecimal roomSubTotal;
    private double basePricePerNight;
    private double totalExtraFeePerNight;
}