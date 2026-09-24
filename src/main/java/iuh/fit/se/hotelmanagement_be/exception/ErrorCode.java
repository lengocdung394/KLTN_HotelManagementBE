package iuh.fit.se.hotelmanagement_be.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // --- SYSTEM & AUTH ERRORS ---
    ORDER_ALREADY_CANCELLED(1043, "Hóa đơn này đã bị hủy, không thể thanh toán", HttpStatus.BAD_REQUEST),
   // ORDER_ALREADY_PAID(1041, "Hóa đơn này đã được thanh toán trước đó", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_AMOUNT(1044, "Số tiền thanh toán không hợp lệ", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_PAYMENT(1040, "Số tiền khách đưa không đủ để thanh toán hóa đơn", HttpStatus.BAD_REQUEST),
   // ORDER_ALREADY_PAID(1041, "Hóa đơn này đã được thanh toán trước đó", HttpStatus.BAD_REQUEST),
    //ORDER_NOT_FOUND(1042, "Không tìm thấy thông tin hóa đơn", HttpStatus.NOT_FOUND),
    INVALID_OTP(1003, "[1003] Invalid or expired OTP", HttpStatus.BAD_REQUEST),
    NEW_CUSTOMER(1000, "[10xx] New customer registration", HttpStatus.OK), // Nếu cần trả về dạng thông báo
    WALK_IN_CUSTOMER_NEEDS_PASSWORD(1001, "[10yy] Walk-in customer needs to set password", HttpStatus.OK),
    CUSTOMER_ALREADY_REGISTERED(1002, "[1002] Customer already registered", HttpStatus.BAD_REQUEST),
    UNCATEGORIZED(9999, "[9999] Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR),
    USERNAME_EXISTED(1001, "[1001] Username existed", HttpStatus.BAD_REQUEST),
    USERNAME_NOT_FOUND(1002, "[1002] Username not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(1003, "[1003] User not found", HttpStatus.NOT_FOUND),
    EMAIL_EXISTED(1004, "[1004] Email already exists", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(4003, "[4003] Access denied", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED(4004, "[4004] Unauthenticated", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(3005, "[3005] Session expired", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(3006, "[3006] Invalid credentials", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_MATCH(3007, "[3007] Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    CURRENT_PASSWORD_INCORRECT(3008, "[3008] Mật khẩu hiện tại không chính xác", HttpStatus.BAD_REQUEST),
    EMPLOYEE_NOT_FOUND(5003, "[5003] Employee not found", HttpStatus.NOT_FOUND),
    // --- HOTEL ERRORS (5xxx) ---
    HOTEL_NOT_FOUND(5001, "[5001] Hotel branch not found", HttpStatus.NOT_FOUND),
    MANAGER_HOTEL_NOT_ASSIGNED(5002, "[5002] Manager account is not assigned to any hotel branch", HttpStatus.BAD_REQUEST),
    //--- PROMOTION ERRORS (6xxx) ---
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

    UNAUTHORIZED_PROMOTION(5012, "[5012] You do not have permission to use this promotion code", HttpStatus.FORBIDDEN),

    PROMOTION_ALREADY_USED(5007, "[5007] This exclusive promotion code has already been used", HttpStatus.BAD_REQUEST),

    PROMOTION_OUT_OF_STOCK(5009, "[5009] Promotion code usage limit has been reached", HttpStatus.BAD_REQUEST),
    PROMOTION_MIN_ORDER_NOT_MET(5010, "[5010] Order value does not meet the minimum requirement for this promotion", HttpStatus.BAD_REQUEST),
    PROMOTION_INACTIVE(5011, "[5011] Promotion is currently not active", HttpStatus.BAD_REQUEST),
    // --- BOOKING ERRORS (8xxx) ---
    BOOKING_NOT_FOUND(8001, "[8001] Booking not found", HttpStatus.NOT_FOUND),
    MULTIPLE_PROMOTIONS_NOT_ALLOWED(8002, "[8002] Only one promotion or discount can be applied per booking", HttpStatus.BAD_REQUEST),
//    EMAIL_EXISTED(1001, "Email is already in use", HttpStatus.BAD_REQUEST),
    PHONE_EXISTED(1002, "This phone number has already been registered!", HttpStatus.BAD_REQUEST),
    EMAIL_OTP_PENDING(1003, "This email is currently pending OTP verification.", HttpStatus.BAD_REQUEST),
    PHONE_OTP_PENDING(1004, "This phone number is pending verification by another request.", HttpStatus.BAD_REQUEST),
    CCCD_EXISTED(1005, "This ID card number (CCCD) is already in use within the system!", HttpStatus.BAD_REQUEST),

    //otp
    OTP_NOT_FOUND(1021, "[1021] Không tìm thấy thông tin hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_VERIFIED(1022, "[1022] Tài khoản đã được xác thực thành công trước đó", HttpStatus.BAD_REQUEST),
    OTP_LOCKED(1023, "[1023] Mã OTP đã bị khóa do nhập sai quá 3 lần", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1024, "[1024] Mã OTP đã hết hạn, vui lòng yêu cầu gửi lại", HttpStatus.BAD_REQUEST),
    OTP_INCORRECT(1025, "[1025] Mã OTP không chính xác", HttpStatus.BAD_REQUEST),
    OTP_INCORRECT_1_ATTEMPT(1025, "[1025] Mã OTP không chính xác. Bạn còn 2 lần thử", HttpStatus.BAD_REQUEST),
    OTP_INCORRECT_2_ATTEMPTS(1026, "[1026] Mã OTP không chính xác. Bạn còn 1 lần thử", HttpStatus.BAD_REQUEST),
    //
    INVALID_IMAGE(1020, "[1020]Image list is invalid or empty", HttpStatus.BAD_REQUEST),
    FLOOR_NOT_FOUND(1021, "[1021] Floor not found", HttpStatus.NOT_FOUND),
    INVALID_IMAGE_COUNT(1022, "[1022]Room must have between 4 and 8 images", HttpStatus.BAD_REQUEST),
    // Bên trong enum ErrorCode của bạn
    INVALID_CHECKOUT_DATE(1099, "Check-out date must be after check-in date", HttpStatus.BAD_REQUEST),
    // Nhóm lỗi về Khách hàng (4000 - 4099)
    CUSTOMER_NOT_FOUND(4001, "[4001] Customer not found", HttpStatus.NOT_FOUND),
    // --- LỖI LIÊN QUAN ĐẾN SỨC CHỨA PHÒNG ---
    EXCEEDS_MAX_INFANTS(1050, "Số lượng em bé vượt quá giới hạn tối đa của phòng", HttpStatus.BAD_REQUEST),
    EXCEEDS_MAX_CAPACITY(1051, "Tổng số lượng khách (người lớn và trẻ em) vượt quá sức chứa tối đa của phòng", HttpStatus.BAD_REQUEST),
    SERVICE_NOT_FOUND(5005, "[5005] Service not found", HttpStatus.NOT_FOUND),
    EXCEEDS_MAX_EXTRA_GUESTS(400, "[400]The number of extra guests exceeds the maximum allowed extra capacity for this room type",HttpStatus.BAD_REQUEST ),
    // bookingdetail
    CANNOT_UPDATE_CANCELLED_ROOM(400, "Không thể cập nhật thông tin của phòng đã bị hủy.", HttpStatus.BAD_REQUEST),
    BOOKING_DETAILS_REQUIRED(5004, "[5004] Booking details cannot be empty", HttpStatus.BAD_REQUEST),
    // Ví dụ các lỗi hỗ trợ khác đã nhắc tới trước đó:
    INVALID_BOOKING_DATE(1052, "Thời gian nhận phòng phải trước thời gian trả phòng", HttpStatus.BAD_REQUEST),
    ROOM_NOT_FOUND(1053, "Không tìm thấy thông tin phòng", HttpStatus.NOT_FOUND),
    ROOM_ALREADY_BOOKED(1054, "Phòng đã có người đặt trong khoảng thời gian này", HttpStatus.CONFLICT),
    BRANCH_POLICY_NOT_FOUND(1055, "Không tìm thấy chính sách giá cho loại phòng này tại chi nhánh", HttpStatus.NOT_FOUND),
    CANNOT_CANCEL_SERVICE_IN_CANCELLED_ROOM(1002, "Cannot cancel service because the room booking has already been cancelled!", HttpStatus.BAD_REQUEST),
    BOOKING_SERVICE_DETAIL_NOT_FOUND(1003, "Booking service detail profile could not be found within this room!", HttpStatus.BAD_REQUEST),

    CANNOT_CHANGE_CANCELLED_ROOM(1004, "Cannot change a room that has already been cancelled", HttpStatus.BAD_REQUEST),
    CANNOT_UPDATE_DATE_FOR_CANCELLED_ROOM(1005, "Cannot update dates for a room that has already been cancelled", HttpStatus.BAD_REQUEST),
    CANNOT_ADD_SERVICE_TO_CANCELLED_ROOM(400, "Cannot add service to a cancelled room detail", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(1001, "Hotel order profile could not be found!", HttpStatus.BAD_REQUEST),
    ORDER_NOT_OPEN(1002, "This hotel order is not currently open for billing updates!", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_PAID(1003, "This room order has already been completely paid for!", HttpStatus.BAD_REQUEST),
    WEBHOOK_VERIFICATION_FAILED(1004, "PayOS secure webhook checksum verification failed!", HttpStatus.BAD_REQUEST),
    PAYMENT_CODE_GENERATION_FAILED(1005, "PayOS secure webhook checksum verification failed!", HttpStatus.BAD_REQUEST ),
    // --- ĐỊNH NGHĨA THEO PHONG CÁCH CỦA BẠN ---

    BOOKING_DETAIL_NOT_FOUND(2002, "[2002] Chi tiết phòng đặt không tồn tại hoặc không tìm thấy", HttpStatus.NOT_FOUND),

    BOOKING_ALREADY_CANCELLED(2004, "[2004] Đơn đặt phòng này đã bị hủy trước đó, không thể chỉnh sửa thêm", HttpStatus.BAD_REQUEST),

    PAID_SERVICE_CANNOT_BE_MODIFIED(4002, "[4002] Dịch vụ đã thanh toán, nghiêm cấm chỉnh sửa số lượng để đối soát kế toán", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}