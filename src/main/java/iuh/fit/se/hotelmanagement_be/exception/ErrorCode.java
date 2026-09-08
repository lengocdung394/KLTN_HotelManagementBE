package iuh.fit.se.hotelmanagement_be.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    UNCATEGORIZED(9999, "[9999] Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR),
    USERNAME_EXISTED(1001, "[1001] Username existed", HttpStatus.BAD_REQUEST),
    USERNAME_NOT_FOUND(1002, "[1002] Username not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(1003, "[1003] User not found", HttpStatus.NOT_FOUND),
    EMAIL_EXISTED(1004, "[1004] Email already exists", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(4003, "[4003] Access denied", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED(4004, "[4004] Unauthenticated", HttpStatus.UNAUTHORIZED),

    SESSION_EXPIRED(3005, "[3005] Session expired", HttpStatus.UNAUTHORIZED), INVALID_CREDENTIALS(3006,"[3006]" , HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}