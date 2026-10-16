package iuh.fit.se.hotelmanagement_be.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private ErrorCode errorCode;

    public AppException(ErrorCode code) {
        super(code.getMessage());
        this.errorCode = code;
    }

    public AppException(ErrorCode code, String message) {
        super(message);
        this.errorCode = code;
    }

}
