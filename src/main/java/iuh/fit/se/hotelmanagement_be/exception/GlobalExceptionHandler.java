package iuh.fit.se.hotelmanagement_be.exception;


import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Hứng các ngoại lệ nghiệp vụ tự định nghĩa (AppException)
    @ExceptionHandler(AppException.class)
    public ResponseEntity<iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse<?>> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.error("AppException occurred: [{}] {}", errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    // 2. Hứng lỗi Validation từ Request DTO (@Valid, @NotBlank, @NotNull, @Min, ...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();

        // Lấy danh sách tất cả các trường vi phạm Validation
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed for request: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse.<Map<String, String>>builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message("Dữ liệu đầu vào không hợp lệ")
                        .result(errors)
                        .build());
    }

    // 3. Hứng lỗi Phân quyền Spring Security (403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse<?>> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    // 4. Hứng tất cả các ngoại lệ chưa được định nghĩa khác (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse<?>> handleUncategorizedException(Exception exception) {
        log.error("Uncategorized Exception: ", exception);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED;

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage() + ": " + exception.getMessage())
                        .build());
    }
}