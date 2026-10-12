package iuh.fit.se.hotelmanagement_be.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // ==========================================
    // 1. SYSTEM, IMAGE & NOTIFICATION ERRORS (1000 - 1999)
    // ==========================================
    UNCATEGORIZED(1999, "[1999] Lỗi không xác định hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_IMAGE(1020, "[1020] Danh sách hình ảnh không hợp lệ hoặc bị trống", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_FORMAT(1006, "Định dạng ảnh không hợp lệ (Chỉ hỗ trợ file ảnh định dạng JPG, JPEG, PNG, WEBP)", HttpStatus.BAD_REQUEST),
    IMAGE_SIZE_TOO_LARGE(1007, "Dung lượng ảnh vượt quá giới hạn cho phép (Tối đa 5MB cho mỗi file)", HttpStatus.BAD_REQUEST),

    ACCOUNT_NOT_ACCESS(1030, "Tài khoản không có quyền truy cập thông báo của khách sạn này", HttpStatus.BAD_REQUEST),
    NOTIFICATION_ROLE(1031, "Thông báo chỉ dành cho nhân viên khách sạn và quản trị viên cấp cao", HttpStatus.BAD_REQUEST),
    NOTIFICATION_SCOPE_INVALID(1032, "Thông báo nằm ngoài phạm vi của tài khoản hiện tại", HttpStatus.BAD_REQUEST),


    // ==========================================
    // 2. AUTH, ACCOUNT, PERMISSION & OTP ERRORS (2000 - 2999)
    // ==========================================
    UNAUTHENTICATED(2001, "[2001] Chưa xác thực danh tính", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(2002, "[2002] Từ chối truy cập (Không có quyền)", HttpStatus.FORBIDDEN),
    SESSION_EXPIRED(2003, "[2003] Phiên đăng nhập đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(2004, "[2004] Thông tin đăng nhập không hợp lệ", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_MATCH(2005, "[2005] Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    CURRENT_PASSWORD_INCORRECT(2006, "[2006] Mật khẩu hiện tại không chính xác", HttpStatus.BAD_REQUEST),

    USERNAME_EXISTED(2010, "[2010] Tên đăng nhập đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_NOT_FOUND(2011, "[2011] Không tìm thấy tên đăng nhập", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(2012, "[2012] Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    EMAIL_EXISTED(2013, "[2013] Email đã được sử dụng", HttpStatus.BAD_REQUEST),
    PHONE_EXISTED(2014, "Số điện thoại này đã được đăng ký trong hệ thống!", HttpStatus.BAD_REQUEST),
    CCCD_EXISTED(2015, "Số căn cước công dân (CCCD) này đã được sử dụng!", HttpStatus.BAD_REQUEST),

    ROLE_NOT_FOUND(2020, "Không tìm thấy vai trò (role)", HttpStatus.BAD_REQUEST),
    PERMISSION_NOT_FOUND(2021, "Không tìm thấy phân quyền (permission)", HttpStatus.BAD_REQUEST),
    PERMISSION_EXIST(2022, "Phân quyền này đã tồn tại trước đó", HttpStatus.BAD_REQUEST),
    PERMISSION_NOT_CATEGORY(2023, "Phân quyền phải đi kèm với phân loại", HttpStatus.BAD_REQUEST),
    ROLE_NOT_PERMISSION(2024, "Vai trò (role) không được gán bất kỳ phân quyền nào", HttpStatus.BAD_REQUEST),
    PERMISSION_NOT_NAME(2025, "Phân quyền bắt buộc phải có tên", HttpStatus.BAD_REQUEST),
    PERMISSION_NOT_CODE(2026, "Phân quyền bắt buộc phải có mã code", HttpStatus.BAD_REQUEST),
    PERMISSION_CODE_NOTVALID(2027, "Mã phân quyền không hợp lệ", HttpStatus.BAD_REQUEST),

    INVALID_OTP(2050, "[2050] Mã OTP không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_NOT_FOUND(2051, "[2051] Không tìm thấy thông tin xác thực OTP hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_VERIFIED(2052, "[2052] Tài khoản đã được xác thực thành công trước đó", HttpStatus.BAD_REQUEST),
    OTP_LOCKED(2053, "[2053] Mã OTP đã bị khóa do nhập sai quá 3 lần", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(2054, "[2054] Mã OTP đã hết hạn, vui lòng yêu cầu gửi lại", HttpStatus.BAD_REQUEST),
    OTP_INCORRECT(2055, "[2055] Mã OTP không chính xác", HttpStatus.BAD_REQUEST),
    OTP_INCORRECT_1_ATTEMPT(2056, "[2056] Mã OTP không chính xác. Bạn còn 2 lần thử", HttpStatus.BAD_REQUEST),
    OTP_INCORRECT_2_ATTEMPTS(2057, "[2057] Mã OTP không chính xác. Bạn còn 1 lần thử", HttpStatus.BAD_REQUEST),
    EMAIL_OTP_PENDING(2058, "Email này hiện đang chờ xác thực mã OTP.", HttpStatus.BAD_REQUEST),
    PHONE_OTP_PENDING(2059, "Số điện thoại này đang chờ xác thực bởi một yêu cầu khác.", HttpStatus.BAD_REQUEST),


    // ==========================================
    // 3. CUSTOMER ERRORS (3000 - 3999)
    // ==========================================
    NEW_CUSTOMER(3001, "[3001] Đăng ký khách hàng mới", HttpStatus.OK),
    WALK_IN_CUSTOMER_NEEDS_PASSWORD(3002, "[3002] Khách vãng lai cần thiết lập mật khẩu", HttpStatus.OK),
    CUSTOMER_ALREADY_REGISTERED(3003, "[3003] Khách hàng đã được đăng ký từ trước", HttpStatus.BAD_REQUEST),
    CUSTOMER_NOT_FOUND(3004, "[3004] Không tìm thấy thông tin khách hàng", HttpStatus.NOT_FOUND),


    // ==========================================
    // 4. HOTEL, BRANCH, BUILDING & FLOOR (4000 - 4999)
    // ==========================================
    HOTEL_NOT_FOUND(4001, "[4001] Không tìm thấy chi nhánh khách sạn", HttpStatus.NOT_FOUND),
    MANAGER_HOTEL_NOT_ASSIGNED(4002, "[4002] Tài khoản quản lý chưa được phân công quản lý chi nhánh khách sạn nào", HttpStatus.BAD_REQUEST),
    EMPLOYEE_NOT_FOUND(4003, "[4003] Không tìm thấy thông tin nhân viên", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_BRANCH_ACCESS(4004, "Bạn không có quyền thao tác trên chi nhánh khách sạn khác", HttpStatus.FORBIDDEN),

    INVALID_BRANCH_NAME(4010, "Tên chi nhánh không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_ADDRESS(4011, "Địa chỉ không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_PHONE(4012, "Số điện thoại không hợp lệ hoặc không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_PROVINCE_NAME(4013, "Tên tỉnh/thành phố không được để trống", HttpStatus.BAD_REQUEST),
    ADDRESS_EXISTED(4014, "Địa chỉ này đã được sử dụng bởi một chi nhánh khác", HttpStatus.BAD_REQUEST),
    BRANCH_EXISTED(4015, "Chi nhánh này đã tồn tại trong hệ thống", HttpStatus.BAD_REQUEST),
    PROVINCE_NOT_FOUND(4016, "[4016] Không tìm thấy tỉnh/thành phố", HttpStatus.NOT_FOUND),

    BUILDING_ALREADY_EXISTS(4020, "Tòa nhà này đã tồn tại", HttpStatus.BAD_REQUEST),
    BUILDING_ID_ALREADY_EXISTS(4021, "Mã tòa nhà này đã tồn tại", HttpStatus.BAD_REQUEST),
    BUILDING_NAME_REQUIRED(4022, "Tên tòa nhà không hợp lệ", HttpStatus.BAD_REQUEST),
    BUILDING_ID_REQUIRED(4023, "Mã ID tòa nhà không hợp lệ", HttpStatus.BAD_REQUEST),
    BUILDING_NOT_FOUND(4024, "Tòa nhà không tồn tại", HttpStatus.NOT_FOUND),

    FLOOR_NUMBER_REQUIRED(4030, "Số tầng không hợp lệ", HttpStatus.BAD_REQUEST),
    FLOOR_ALREADY_EXISTS(4031, "Tầng này đã tồn tại trong tòa nhà", HttpStatus.BAD_REQUEST),
    FLOOR_NUMBER_ALREADY_EXISTS(4032, "Số tầng này đã tồn tại", HttpStatus.BAD_REQUEST),
    FLOOR_NOT_FOUND(4033, "[4033] Không tìm thấy thông tin tầng", HttpStatus.NOT_FOUND),


    // ==========================================
    // 5. ROOM, CAPACITY & POLICY ERRORS (5000 - 5999)
    // ==========================================
    ROOM_NOT_FOUND(5001, "Không tìm thấy thông tin phòng", HttpStatus.NOT_FOUND),
    ROOM_NUMBER_ALREADY_EXISTS(5002, "Số phòng này đã tồn tại", HttpStatus.BAD_REQUEST),
    ROOM_ALREADY_BOOKED(5003, "Phòng này đã được đặt trước", HttpStatus.BAD_REQUEST),
    ROOM_IS_BEING_LOCKED(5004, "Phòng này đang được giữ chỗ tạm thời", HttpStatus.BAD_REQUEST),
    ROOM_ALREADY_OCCUPIED_OR_BOOKED(5005, "Phòng này hiện đang có khách ở hoặc đã bị trùng lịch đặt phòng!", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_COUNT(5006, "[5006] Mỗi phòng phải có từ 4 đến 8 hình ảnh", HttpStatus.BAD_REQUEST),

    EXCEEDS_MAX_INFANTS(5010, "Số lượng em bé vượt quá giới hạn tối đa của phòng", HttpStatus.BAD_REQUEST),
    EXCEEDS_MAX_CAPACITY(5011, "Tổng số lượng khách (người lớn và trẻ em) vượt quá sức chứa tối đa của phòng", HttpStatus.BAD_REQUEST),
    EXCEEDS_MAX_EXTRA_GUESTS(5012, "[5012] Số lượng khách phụ thu vượt quá giới hạn cho phép của loại phòng này", HttpStatus.BAD_REQUEST),

    ROOM_POLICY_REQUIRED(5020, "Cần gửi ít nhất một cấu hình loại phòng", HttpStatus.BAD_REQUEST),
    INVALID_ROOM_POLICY_DATA(5021, "Mỗi loại phòng cần đầy đủ thông tin giá, diện tích, sức chứa và phụ thu", HttpStatus.BAD_REQUEST),
    DUPLICATE_ROOM_POLICY_TYPE(5022, "Không được gửi trùng loại phòng", HttpStatus.BAD_REQUEST),
    INVALID_ROOM_POLICY_COUNT(5023, "Cần cấu hình chính xác cho đủ 4 loại phòng", HttpStatus.BAD_REQUEST),
    INVALID_ROOM_POLICY_TYPES(5024, "Danh sách loại phòng không hợp lệ. Phải bao gồm đủ 4 loại phòng theo quy định", HttpStatus.BAD_REQUEST),
    BRANCH_POLICY_NOT_FOUND(5025, "Không tìm thấy chính sách giá cho loại phòng này tại chi nhánh", HttpStatus.NOT_FOUND),
    POLICY_NOT_FOUND(5026, "Không tìm thấy chính sách phòng với ID tương ứng", HttpStatus.NOT_FOUND),
    DUPLICATE_SEASONAL_RATE(5027, "Đã tồn tại đợt giá mùa vụ cho loại phòng này trong khoảng thời gian đã chọn", HttpStatus.BAD_REQUEST),


    // ==========================================
    // 6. BOOKING & BOOKING DETAIL ERRORS (6000 - 6999)
    // ==========================================
    BOOKING_NOT_FOUND(6001, "[6001] Không tìm thấy thông tin đặt phòng", HttpStatus.NOT_FOUND),
    BOOKING_DETAIL_NOT_FOUND(6002, "[6002] Chi tiết phòng đặt không tồn tại hoặc không tìm thấy", HttpStatus.NOT_FOUND),
    BOOKING_DETAILS_REQUIRED(6003, "[6003] Chi tiết đặt phòng không được để trống", HttpStatus.BAD_REQUEST),
    BOOKING_ALREADY_CANCELLED(6004, "[6004] Đơn đặt phòng này đã bị hủy trước đó, không thể chỉnh sửa thêm", HttpStatus.BAD_REQUEST),

    INVALID_BOOKING_DATE(6010, "Thời gian nhận phòng phải trước thời gian trả phòng", HttpStatus.BAD_REQUEST),
    INVALID_CHECKOUT_DATE(6011, "Thời gian trả phòng phải sau thời gian nhận phòng", HttpStatus.BAD_REQUEST),

    CANNOT_UPDATE_CANCELLED_ROOM(6020, "Không thể cập nhật thông tin của phòng đã bị hủy.", HttpStatus.BAD_REQUEST),
    CANNOT_CHANGE_CANCELLED_ROOM(6021, "Không thể thay đổi phòng đã bị hủy", HttpStatus.BAD_REQUEST),
    CANNOT_UPDATE_DATE_FOR_CANCELLED_ROOM(6022, "Không thể cập nhật thời gian cho phòng đã bị hủy", HttpStatus.BAD_REQUEST),
    CANNOT_ADD_SERVICE_TO_CANCELLED_ROOM(6023, "Không thể thêm dịch vụ vào chi tiết phòng đã bị hủy", HttpStatus.BAD_REQUEST),
    CANNOT_CANCEL_SERVICE_IN_CANCELLED_ROOM(6024, "Không thể hủy dịch vụ vì phòng đặt này đã bị hủy trước đó!", HttpStatus.BAD_REQUEST),


    // ==========================================
    // 7. SERVICE ERRORS (7000 - 7999)
    // ==========================================
    SERVICE_NOT_FOUND(7001, "[7001] Không tìm thấy dịch vụ", HttpStatus.NOT_FOUND),
    BOOKING_SERVICE_DETAIL_NOT_FOUND(7002, "Không tìm thấy chi tiết dịch vụ đi kèm trong phòng này!", HttpStatus.BAD_REQUEST),
    PAID_SERVICE_CANNOT_BE_MODIFIED(7003, "[7003] Dịch vụ đã thanh toán, nghiêm cấm chỉnh sửa số lượng để đối soát kế toán", HttpStatus.BAD_REQUEST),


    // ==========================================
    // 8. PROMOTION & VOUCHER ERRORS (8000 - 8999)
    // ==========================================
    PROMOTION_NOT_FOUND(8001, "[8001] Không tìm thấy chương trình khuyến mãi", HttpStatus.NOT_FOUND),
    PROMOTION_CODE_EXISTED(8002, "[8002] Mã khuyến mãi đã tồn tại", HttpStatus.BAD_REQUEST),
    PROMOTION_EXPIRED(8003, "[8003] Chương trình khuyến mãi đã hết hạn hoặc chưa bắt đầu", HttpStatus.BAD_REQUEST),
    PROMOTION_USAGE_LIMIT_EXCEEDED(8004, "[8004] Đã đạt giới hạn số lần sử dụng mã khuyến mãi", HttpStatus.BAD_REQUEST),
    PROMOTION_INVALID_DATES(8005, "[8005] Ngày bắt đầu phải trước ngày kết thúc và không được ở trong quá khứ", HttpStatus.BAD_REQUEST),
    PROMOTION_INVALID_DISCOUNT_VALUE(8006, "[8006] Phần trăm giảm giá không thể vượt quá 100%", HttpStatus.BAD_REQUEST),
    PROMOTION_NOT_APPLICABLE_HOTEL(8007, "[8007] Mã khuyến mãi không áp dụng cho chi nhánh khách sạn này", HttpStatus.BAD_REQUEST),
    PROMOTION_MIN_BOOKING_VALUE_NOT_MET(8008, "[8008] Tổng giá trị đặt phòng chưa đạt mức tối thiểu để áp dụng mã này", HttpStatus.BAD_REQUEST),

    CUSTOMER_PROMOTION_NOT_FOUND(8020, "[8020] Không tìm thấy mã voucher của khách hàng", HttpStatus.NOT_FOUND),
    CUSTOMER_PROMOTION_ALREADY_USED(8021, "[8021] Mã voucher của khách hàng đã được sử dụng trước đó", HttpStatus.BAD_REQUEST),
    CUSTOMER_PROMOTION_NOT_BELONG_TO_CUSTOMER(8022, "[8022] Mã voucher này không thuộc về khách hàng hiện tại", HttpStatus.FORBIDDEN),

    UNAUTHORIZED_PROMOTION(8030, "[8030] Bạn không có quyền sử dụng mã khuyến mãi này", HttpStatus.FORBIDDEN),
    PROMOTION_ALREADY_USED(8031, "[8031] Mã khuyến mãi độc quyền này đã được sử dụng", HttpStatus.BAD_REQUEST),
    PROMOTION_OUT_OF_STOCK(8032, "[8032] Mã khuyến mãi đã hết lượt sử dụng", HttpStatus.BAD_REQUEST),
    PROMOTION_MIN_ORDER_NOT_MET(8033, "[8033] Giá trị hóa đơn chưa đạt mức tối thiểu để áp dụng khuyến mãi", HttpStatus.BAD_REQUEST),
    PROMOTION_INACTIVE(8034, "[8034] Chương trình khuyến mãi hiện đang không hoạt động", HttpStatus.BAD_REQUEST),
    MULTIPLE_PROMOTIONS_NOT_ALLOWED(8035, "[8035] Chỉ được phép áp dụng một mã khuyến mãi hoặc giảm giá cho mỗi đơn đặt phòng", HttpStatus.BAD_REQUEST),

    PROMOTION_MIN_ROOM_NOT_MET(8040, "Tiền phòng chưa đạt mức tối thiểu để áp dụng mã này.", HttpStatus.BAD_REQUEST),
    PROMOTION_MIN_SERVICE_NOT_MET(8041, "Tiền dịch vụ chưa đạt mức tối thiểu để áp dụng mã này.", HttpStatus.BAD_REQUEST),


    // ==========================================
    // 9. BILLING, ORDER & PAYMENT ERRORS (9000 - 9999)
    // ==========================================
    ORDER_NOT_FOUND(9001, "Không tìm thấy thông tin hóa đơn đặt phòng của khách sạn!", HttpStatus.BAD_REQUEST),
    ORDER_NOT_OPEN(9002, "Hóa đơn này hiện không mở để cập nhật thanh toán!", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_PAID(9003, "Hóa đơn phòng này đã được thanh toán hoàn tất trước đó!", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_CANCELLED(9004, "Hóa đơn này đã bị hủy, không thể thanh toán", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_CLOSED(9005, "[9005] Hóa đơn này đã được quyết toán và đóng", HttpStatus.BAD_REQUEST),

    INVALID_PAYMENT_AMOUNT(9010, "Số tiền thanh toán không hợp lệ", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_PAYMENT(9011, "Số tiền khách đưa không đủ để thanh toán hóa đơn", HttpStatus.BAD_REQUEST),
    PAYMENT_EXCEEDS_REMAINING(9012, "[9012] Số tiền thanh toán vượt quá số tiền còn nợ của hóa đơn", HttpStatus.BAD_REQUEST),
    WEBHOOK_VERIFICATION_FAILED(9020, "Xác thực chữ ký bảo mật Webhook PayOS thất bại!", HttpStatus.BAD_REQUEST),
    PAYMENT_CODE_GENERATION_FAILED(9021, "Tạo mã thanh toán PayOS thất bại!", HttpStatus.BAD_REQUEST);
    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}