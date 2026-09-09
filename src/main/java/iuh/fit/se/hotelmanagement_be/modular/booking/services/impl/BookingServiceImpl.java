package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Employee;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.CustomerRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.EmployeeRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.BookingDetailCreateRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.BookingResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.room.entities.Room;
import iuh.fit.se.hotelmanagement_be.modular.room.repositories.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingServiceImpl implements BookingService {
    BookingRepository bookingRepository;
    CustomerRepository customerRepository;
    EmployeeRepository employeeRepository;
    RoomRepository roomRepository;
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    RoomPricingCalculator roomPricingCalculator;

    /**
     * LUỒNG 1: KHÁCH HÀNG TỰ ĐẶT ONLINE
     */
    @Transactional
    @Override
    public BookingResponse createBooking(BookingCreateRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        // Khởi tạo Booking online
        Booking booking = Booking.builder()
                .customer(customer)
                .bookingChannel(request.getBookingChannel() != null ? request.getBookingChannel() : BookingChannel.ONLINE)
                .bookingStatus(BookingStatus.PENDING)
                .build();

        processBookingDetailsAndSave(booking, request.getBookingDetails());

        Booking savedBooking = bookingRepository.save(booking);
        return toBookingResponse(savedBooking);
    }

    /**
     * LUỒNG 2: NHÂN VIÊN ĐẶT TẠI QUẦY (COUNTER / OFFLINE)
     */
    @Transactional
    public BookingResponse createCounterBooking(Long employeeId, BookingCreateRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        // Lấy thông tin nhân viên thao tác tại quầy
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        // Khởi tạo Booking tại quầy
        Booking booking = Booking.builder()
                .customer(customer)
                .employee(employee) // Gắn nhân viên lập đơn
                .bookingChannel(BookingChannel.OFFLINE)
                .bookingStatus(BookingStatus.CONFIRMED) // Tại quầy thường xác nhận luôn
                .build();

        processBookingDetailsAndSave(booking, request.getBookingDetails());

        Booking savedBooking = bookingRepository.save(booking);
        return toBookingResponse(savedBooking);
    }

    /**
     * HÀM HELPER CHUNG: Xử lý lặp qua danh sách phòng, tính toán giá và tạo BookingDetail
     */
    private void processBookingDetailsAndSave(Booking booking, List<BookingDetailCreateRequest> detailRequests) {
        for (BookingDetailCreateRequest detailReq : detailRequests) {
            Room room = roomRepository.findById(detailReq.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

            // Lấy policy của chi nhánh chứa phòng này
            Long hotelId = room.getFloor().getBuilding().getHotel().getId();
            BranchRoomPolicy policy = branchRoomPolicyRepository
                    .findByHotelIdAndRoomType(hotelId, room.getRoomType());

            if (policy == null) {
                throw new AppException(ErrorCode.BRANCH_POLICY_NOT_FOUND);
            }

            // Tính phụ thu người cho riêng phòng này
            double extraFeePerNight = roomPricingCalculator.calculateExtraFeeWithCapacityWeight(
                    policy,
                    detailReq.getNumAdults(),
                    detailReq.getNumChildren(),
                    detailReq.getNumInfants()
            );

            long nights = ChronoUnit.DAYS.between(
                    detailReq.getCheckInTime().toLocalDate(),
                    detailReq.getCheckOutTime().toLocalDate()
            );
            if (nights == 0) nights = 1;

            double roomTotalPrice = (room.calculateTotalPrice() + extraFeePerNight) * nights;

            // Tạo BookingDetail liên kết trực tiếp với Room và Booking
            BookingDetail bookingDetail = BookingDetail.builder()
                    .room(room)
                    .checkinTime(detailReq.getCheckInTime())
                    .checkoutTime(detailReq.getCheckOutTime())
                    .numAdults(detailReq.getNumAdults())
                    .numChildren(detailReq.getNumChildren())
                    .numInfants(detailReq.getNumInfants())
                    .price(roomTotalPrice)
                    .build();

            // Thêm vào quan hệ 2 chiều
            booking.addBookingDetail(bookingDetail);
        }
    }

    /**
     * HÀM MAPPER NỘI BỘ
     */
    private BookingResponse toBookingResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        List<BookingDetailResponse> detailResponses = null;

        if (booking.getBookingDetails() != null) {
            detailResponses = booking.getBookingDetails().stream().map(detail ->
                    BookingDetailResponse.builder()
                            .bookingDetailId(detail.getId())
                            .roomId(detail.getRoom() != null ? detail.getRoom().getId() : null)
                            .roomName(detail.getRoom() != null ? detail.getRoom().getRoomType().toString() : null)
                            .roomTypeName(detail.getRoom() != null ? detail.getRoom().getRoomType().name() : null)
                            .checkInTime(detail.getCheckinTime())
                            .checkOutTime(detail.getCheckoutTime())
                            .numAdults(detail.getNumAdults())
                            .numChildren(detail.getNumChildren())
                            .numInfants(detail.getNumInfants())
                            .price(detail.getPrice())
                            .build()
            ).toList();
        }

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .customerId(booking.getCustomer() != null ? booking.getCustomer().getId() : null)
                .customerName(booking.getCustomer() != null ? booking.getCustomer().getFullName() : null)
                .bookingStatus(booking.getBookingStatus())
                .bookingChannel(booking.getBookingChannel())
                .createdAt(booking.getCreatedAt())
                .bookingDetails(detailResponses)
                .build();
    }
}