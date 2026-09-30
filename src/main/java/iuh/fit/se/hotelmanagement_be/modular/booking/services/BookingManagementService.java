package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.*;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingModificationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface BookingManagementService {
    /**
     * Cập nhật thời gian check-in / check-out (gia hạn hoặc trả phòng sớm) cho một hoặc nhiều phòng hiện có trong đơn đặt phòng.
     * Hệ thống sẽ tính toán lại giá phòng dựa trên dải ngày mới và trả về khoản tiền chênh lệch.
     *
     * @param booking Đối tượng đặt phòng hiện tại cần cập nhật.
     *                cập nhật số lượng người ở phòng mới
     * @return Khoản tiền chênh lệch (dương nếu ở thêm ngày, âm nếu rút ngắn thời gian).
     */

    BigDecimal processRoomUpdates(Booking booking, List<RoomUpdateRequest> updates);

    /**
     * Hàm "nhạc trưởng" điều phối toàn bộ quá trình chỉnh sửa đơn đặt phòng (modify booking).
     * Cho phép thực hiện đồng thời nhiều thao tác trong một giao dịch: Hủy phòng, Thêm phòng mới,
     * Đổi phòng, Đổi ngày, Hủy dịch vụ lẻ; sau đó tự động cập nhật lại tổng tiền và lưu xuống cơ sở dữ liệu.
     *
     * @param bookingId ID của đơn đặt phòng cần chỉnh sửa.
     * @param request   Đối tượng chứa toàn bộ thông tin thay đổi do người dùng gửi lên.
     * @return Yêu cầu chỉnh sửa ban đầu (hoặc DTO kết quả tương ứng).
     */
    BookingModificationResponse modifyBooking(String bookingId, BookingModificationRequest request);

           /**
     * Thêm mới các phòng vào đơn đặt phòng hiện có (có thể kèm theo các dịch vụ đi kèm ngay tại thời điểm thêm).
     * Tự động tính toán giá phòng theo chính sách chi nhánh và giá mùa vụ (Seasonal Rate).
     *
     * @param booking    Đối tượng đặt phòng hiện tại.
     * @param roomsToAdd Danh sách các phòng mới cần thêm kèm thông tin thời gian, số lượng khách và dịch vụ tùy chọn.
     * @return Tổng số tiền phòng và dịch vụ mới được cộng thêm vào đơn.
     */
    BigDecimal processRoomAdditions(Booking booking, List<NewRoomRequest> roomsToAdd);

    /**
     * Hủy bỏ một hoặc nhiều phòng trong đơn đặt phòng, đồng thời tự động hủy luôn các dịch vụ đi kèm chưa thanh toán của các phòng đó.
     *
     * @param booking           Đối tượng đặt phòng hiện tại.
     * @param detailIdsToCancel Danh sách ID chi tiết phòng (`BookingDetail`) cần hủy.
     * @param operatorName      Họ tên nhân viên thực hiện thao tác hủy phòng (dùng để ghi log/kiểm toán).
     * @return Tổng số tiền phòng được giảm trừ sau khi hủy.
     */
    BigDecimal processCancellations(Booking booking, List<String> detailIdsToCancel, String operatorName);

    BigDecimal processServicesForExistingRooms(Booking booking, List<RoomServiceAdditionRequest> requests);

    BigDecimal processServiceQuantityUpdates(Booking booking, List<UpdateServiceQuantityRequest> quantityUpdates);
}
