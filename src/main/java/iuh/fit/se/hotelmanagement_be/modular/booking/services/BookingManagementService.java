package iuh.fit.se.hotelmanagement_be.modular.booking.services;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.*;

import java.math.BigDecimal;
import java.util.List;

public interface BookingManagementService {
    /**
     * Cập nhật thời gian check-in / check-out (gia hạn hoặc trả phòng sớm) cho một hoặc nhiều phòng hiện có trong đơn đặt phòng.
     * Hệ thống sẽ tính toán lại giá phòng dựa trên dải ngày mới và trả về khoản tiền chênh lệch.
     *
     * @param booking     Đối tượng đặt phòng hiện tại cần cập nhật.
     * @param dateUpdates Danh sách các yêu cầu thay đổi ngày (bao gồm ID phòng và mốc thời gian mới).
     * @return Khoản tiền chênh lệch (dương nếu ở thêm ngày, âm nếu rút ngắn thời gian).
     */
    BigDecimal processRoomDateUpdates(Booking booking, List<RoomDateUpdateRequest> dateUpdates);

    /**
     * Hàm "nhạc trưởng" điều phối toàn bộ quá trình chỉnh sửa đơn đặt phòng (modify booking).
     * Cho phép thực hiện đồng thời nhiều thao tác trong một giao dịch: Hủy phòng, Thêm phòng mới,
     * Đổi phòng, Đổi ngày, Hủy dịch vụ lẻ; sau đó tự động cập nhật lại tổng tiền và lưu xuống cơ sở dữ liệu.
     *
     * @param bookingId ID của đơn đặt phòng cần chỉnh sửa.
     * @param request   Đối tượng chứa toàn bộ thông tin thay đổi do người dùng gửi lên.
     * @return Yêu cầu chỉnh sửa ban đầu (hoặc DTO kết quả tương ứng).
     */
    BookingModificationRequest modifyBooking(Long bookingId, BookingModificationRequest request);

    /**
     * Thực hiện đổi phòng hàng loạt cho các phòng đang có trong đơn đặt phòng (ví dụ: chuyển từ phòng Standard sang Deluxe).
     * Đánh dấu phòng cũ là đã hủy, tạo chi tiết phòng mới với thời gian và số lượng khách giữ nguyên, sau đó tính chênh lệch giá.
     *
     * @param booking     Đối tượng đặt phòng hiện tại.
     * @param roomChanges Danh sách các yêu cầu đổi phòng (gồm ID phòng cũ cần đổi và ID phòng mới muốn chuyển sang).
     * @return Khoản tiền chênh lệch giữa phòng mới và phòng cũ.
     */
    BigDecimal processRoomChanges(Booking booking, List<UpdateRoomChangeRequest> roomChanges);

    /**
     * Hủy bỏ các dịch vụ phát sinh cụ thể theo từng phòng trong đơn đặt phòng mà không cần hủy toàn bộ phòng.
     *
     * @param booking      Đối tượng đặt phòng hiện tại.
     * @param cancellations Danh sách yêu cầu hủy dịch vụ (gom nhóm theo ID phòng và danh sách ID dịch vụ cần hủy).
     * @return Tổng số tiền được khấu trừ/giảm trừ từ việc hủy các dịch vụ này.
     */
    BigDecimal processServiceCancellations(Booking booking, List<ServiceCancellationRequest> cancellations);

    /**
     * Thêm mới các phòng vào đơn đặt phòng hiện có (có thể kèm theo các dịch vụ đi kèm ngay tại thời điểm thêm).
     * Tự động tính toán giá phòng theo chính sách chi nhánh và giá mùa vụ (Seasonal Rate).
     *
     * @param booking     Đối tượng đặt phòng hiện tại.
     * @param roomsToAdd  Danh sách các phòng mới cần thêm kèm thông tin thời gian, số lượng khách và dịch vụ tùy chọn.
     * @return Tổng số tiền phòng và dịch vụ mới được cộng thêm vào đơn.
     */
    BigDecimal processRoomAdditions(Booking booking, List<NewRoomRequest> roomsToAdd);

    /**
     * Hủy bỏ một hoặc nhiều phòng trong đơn đặt phòng, đồng thời tự động hủy luôn các dịch vụ đi kèm chưa thanh toán của các phòng đó.
     *
     * @param booking            Đối tượng đặt phòng hiện tại.
     * @param detailIdsToCancel  Danh sách ID chi tiết phòng (`BookingDetail`) cần hủy.
     * @param operatorName       Họ tên nhân viên thực hiện thao tác hủy phòng (dùng để ghi log/kiểm toán).
     * @return Tổng số tiền phòng được giảm trừ sau khi hủy.
     */
    BigDecimal processCancellations(Booking booking, List<Long> detailIdsToCancel, String operatorName);
}
