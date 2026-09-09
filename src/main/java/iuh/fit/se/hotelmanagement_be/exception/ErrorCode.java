package iuh.fit.se.hotelmanagement_be.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // --- SYSTEM & AUTH ERRORS ---
    UNCATEGORIZED(9999, "[9999] Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR),
    USERNAME_EXISTED(1001, "[1001] Username existed", HttpStatus.BAD_REQUEST),
    USERNAME_NOT_FOUND(1002, "[1002] Username not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(1003, "[1003] User not found", HttpStatus.NOT_FOUND),
    EMAIL_EXISTED(1004, "[1004] Email already exists", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(4003, "[4003] Access denied", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED(4004, "[4004] Unauthenticated", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(3005, "[3005] Session expired", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(3006, "[3006] Invalid credentials", HttpStatus.BAD_REQUEST),

    // --- HOTEL ERRORS (5xxx) ---
    HOTEL_NOT_FOUND(5001, "[5001] Hotel branch not found", HttpStatus.NOT_FOUND),
    MANAGER_HOTEL_NOT_ASSIGNED(5002, "[5002] Manager account is not assigned to any hotel branch", HttpStatus.BAD_REQUEST),

    // --- PROMOTION ERRORS (6xxx) ---
    PROMOTION_NOT_FOUND(6001, "[6001] Promotion program not found", HttpStatus.NOT_FOUND),
    PROMOTION_CODE_EXISTED(6002, "[6002] Promotion code already exists", HttpStatus.BAD_REQUEST),
    PROMOTION_EXPIRED(6003, "[6003] Promotion program has expired or not started yet", HttpStatus.BAD_REQUEST),
    PROMOTION_USAGE_LIMIT_EXCEEDED(6004, "[6004] Promotion usage limit has been reached", HttpStatus.BAD_REQUEST),
    PROMOTION_INVALID_DATES(6005, "[6005] Start date must be before end date and not in the past", HttpStatus.BAD_REQUEST),
    PROMOTION_INVALID_DISCOUNT_VALUE(6006, "[6006] Discount percentage cannot exceed 100%", HttpStatus.BAD_REQUEST),
    PROMOTION_NOT_APPLICABLE_HOTEL(6007, "[6007] Promotion is not applicable for this hotel branch", HttpStatus.BAD_REQUEST),
    PROMOTION_MIN_BOOKING_VALUE_NOT_MET(6008, "[6008] Booking total does not meet the minimum requirement for this promotion", HttpStatus.BAD_REQUEST),

    // --- CUSTOMER PROMOTION ERRORS (7xxx) ---
    CUSTOMER_PROMOTION_NOT_FOUND(7001, "[7001] Customer voucher code not found", HttpStatus.NOT_FOUND),
    CUSTOMER_PROMOTION_ALREADY_USED(7002, "[7002] Customer voucher code has already been used", HttpStatus.BAD_REQUEST),
    CUSTOMER_PROMOTION_NOT_BELONG_TO_CUSTOMER(7003, "[7003] Customer voucher code does not belong to this customer", HttpStatus.FORBIDDEN),

    // --- BOOKING ERRORS (8xxx) ---
    BOOKING_NOT_FOUND(8001, "[8001] Booking not found", HttpStatus.NOT_FOUND),
    MULTIPLE_PROMOTIONS_NOT_ALLOWED(8002, "[8002] Only one promotion or discount can be applied per booking", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}