package iuh.fit.se.hotelmanagement_be.modular.closure.requests;

import iuh.fit.se.hotelmanagement_be.modular.booking.requests.CheckoutRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResolveHotelClosureRequest {
    @NotBlank
    private String action;

    @Valid
    private BookingCancellationRequest cancellation;

    @Valid
    private CheckoutRequest checkout;
}
