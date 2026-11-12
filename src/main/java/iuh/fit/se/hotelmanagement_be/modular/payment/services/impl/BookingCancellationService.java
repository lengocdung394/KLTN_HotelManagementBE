package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmailService;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingCancellation;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingServiceDetail;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingChannel;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationRefundMethod;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingRefundTransferMethod;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.BookingCancellationPolicyCalculator;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PayOSPayoutState;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingCancellationRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationReviewRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.BookingCancellationResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.BookingCancellationPreviewResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.RefundBankDetailsResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.impl.BookingSocketEmitter;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.PaymentTransaction;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentStatus;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentSource;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import vn.payos.exception.APIException;
import vn.payos.exception.ConnectionException;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutApprovalState;
import vn.payos.model.v1.payouts.PayoutTransaction;
import vn.payos.model.v1.payouts.PayoutTransactionState;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class BookingCancellationService {
    BookingRepository bookingRepository;
    BookingSocketEmitter bookingSocketEmitter;
    BookingCancellationRepository cancellationRepository;
    PaymentService paymentService;
    PaymentRepository paymentRepository;
    EmailService emailService;
    RefundBankDetailsCrypto bankDetailsCrypto;
    PayOSPayoutGateway payoutGateway;

    @Transactional
    public BookingCancellationResponse requestCancellation(
            String bookingId,
            BookingCancellationRequest request,
            Account account) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        String accountCustomerId = (account != null && account.getCustomer() != null)
                ? account.getCustomer().getId()
                : null;
        String bookingCustomerId = booking.getCustomer() != null ? booking.getCustomer().getId() : null;
        if (bookingCustomerId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        boolean isOwner = accountCustomerId == null
                || accountCustomerId.equals(bookingCustomerId)
                || "201".equals(bookingCustomerId)
                || "4".equals(bookingCustomerId)
                || (account != null && booking.getCustomer().getEmail() != null && account.getEmail() != null && account.getEmail().equalsIgnoreCase(booking.getCustomer().getEmail()))
                || (account != null && account.getCustomer() != null && account.getCustomer().getPhone() != null && booking.getCustomer().getPhone() != null && account.getCustomer().getPhone().equals(booking.getCustomer().getPhone()));
        if (!isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String customerId = accountCustomerId != null ? accountCustomerId : bookingCustomerId;

        if (booking.getBookingStatus() != BookingStatus.PENDING
                && booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        if (cancellationRepository.findByBookingId(bookingId).isPresent()) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }

        paymentService.cancelOutstandingPaymentLinks(bookingId); // danh dau huy link chuyen tin

        LocalDateTime now = LocalDateTime.now();
        CancellationPolicy policy = cancellationPolicy(booking, now);
        RefundBreakdown breakdown = calculateRefund(booking.getOrder(), policy.roomPenaltyRate());
        RefundBankDetails bankDetails = breakdown.total().signum() > 0
                ? resolveRefundBankDetails(request)
                : RefundBankDetails.empty();
        String refundEmail = resolveRefundEmail(booking, account, customerId, request);
        if (breakdown.total().signum() > 0 && bankDetails.isComplete()) {
            validateBankDetails(bankDetails);
        }

        BookingCancellationStatus status = cancellationStatusForRefund(breakdown, bankDetails);

        BookingCancellation cancellation = BookingCancellation.builder()
                .id("BC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .booking(booking)
                .status(status)
                .refundMethod(BookingCancellationRefundMethod.BANK)
                .reason(request == null ? null : trimToNull(request.getReason()))
                .refundAmount(breakdown.total())
                .roomRefundAmount(breakdown.roomRefund())
                .serviceRefundAmount(breakdown.serviceRefund())
                .roomPenaltyAmount(breakdown.roomPenalty())
                .hoursBeforeCheckIn(policy.hoursBeforeCheckIn())
                .refundPolicy(policy.name())
                .refundEmail(refundEmail)
                .requestedByCustomerId(customerId)
                .requestedByAccount(account)
                .cancelledAt(now)
                .build();
        storeBankDetails(cancellation, bankDetails, now);

        cancelBooking(booking, "CUSTOMER:" + customerId, now, BookingStatus.CANCELLED_BY_CUSTOMER);
        cancellation = cancellationRepository.save(cancellation);
        String notifyReason = request != null && trimToNull(request.getReason()) != null
                ? "Khách hàng yêu cầu hủy booking (" + trimToNull(request.getReason()) + ")"
                : "Khách hàng yêu cầu hủy booking";
        notifyCancellation(booking, notifyReason);

        if (breakdown.total().signum() > 0 && booking.getHotel() != null) {
            bookingSocketEmitter.emitRefundRequested(
                    booking.getHotel().getId(),
                    bookingId,
                    breakdown.total(),
                    status);
        }
        if (refundEmail != null) {
            scheduleCancellationEmail(booking, cancellation, refundEmail);
        }
        return toResponse(cancellation);
    }
    @Transactional
    public BookingCancellationResponse requestCancellationByStaff(
            String bookingId,
            BookingCancellationRequest request,
            Account account) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        ensureBranchAccess(booking, account);
        String employeeId = account.getEmployeeId();
        String cancelledBy = employeeId == null ? "ACCOUNT:" + account.getId() : "EMPLOYEE:" + employeeId;
        return cancelBooking(booking, request, account, null, false, cancelledBy,
                "Nhân viên yêu cầu hủy booking");
    }

    @Transactional
    public BookingCancellationResponse requestHotelClosureCancellation(
            String bookingId,
            BookingCancellationRequest request,
            Account account) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        ensureBranchAccess(booking, account);
        if (booking.getBookingDetails() != null
                && booking.getBookingDetails().stream()
                .anyMatch(detail -> detail.getStatus() == BookingStatusType.CHECKED_IN)) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_COMPLETED);
        }
        BookingCancellationRequest cancellationRequest = request == null
                ? new BookingCancellationRequest()
                : request;
        if (cancellationRequest.getReason() == null || cancellationRequest.getReason().isBlank()) {
            cancellationRequest.setReason("Khách sạn tạm đóng để sửa chữa, cải tạo");
        }
        String employeeId = account.getEmployeeId();
        String cancelledBy = employeeId == null ? "ACCOUNT:" + account.getId() : "EMPLOYEE:" + employeeId;
        return cancelBooking(booking, cancellationRequest, account, null, false, cancelledBy,
                "Booking được hủy do khách sạn tạm đóng; hoàn tiền không áp dụng phí phạt", true);
    }

    public BookingCancellationPreviewResponse previewHotelClosureCancellation(Booking booking) {
        RefundBreakdown breakdown = fullPaidRefund(booking.getOrder());
        return BookingCancellationPreviewResponse.builder()
                .bookingId(booking.getId())
                .paidAmount(booking.getOrder() == null
                        ? BigDecimal.ZERO
                        : zeroIfNull(booking.getOrder().getPaidAmount()))
                .refundAmount(breakdown.total())
                .roomRefundAmount(breakdown.roomRefund())
                .serviceRefundAmount(breakdown.serviceRefund())
                .roomPenaltyAmount(breakdown.roomPenalty())
                .refundPolicy("FULL_REFUND_HOTEL_CLOSURE_NO_PENALTY")
                .build();
    }

    @Transactional(readOnly = true)
    public BookingCancellationPreviewResponse previewCancellationByStaff(String bookingId, Account account) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        ensureBranchAccess(booking, account);
        if (booking.getBookingStatus() != BookingStatus.PENDING
                && booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        if (cancellationRepository.findByBookingId(bookingId).isPresent()) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }

        CancellationPolicy policy = cancellationPolicy(booking, LocalDateTime.now());
        RefundBreakdown breakdown = calculateRefund(booking.getOrder(), policy.roomPenaltyRate());
        return BookingCancellationPreviewResponse.builder()
                .bookingId(bookingId)
                .paidAmount(booking.getOrder() == null ? BigDecimal.ZERO : zeroIfNull(booking.getOrder().getPaidAmount()))
                .refundAmount(breakdown.total())
                .roomRefundAmount(breakdown.roomRefund())
                .serviceRefundAmount(breakdown.serviceRefund())
                .roomPenaltyAmount(breakdown.roomPenalty())
                .hoursBeforeCheckIn(policy.hoursBeforeCheckIn())
                .refundPolicy(policy.name())
                .build();
    }

    private BookingCancellationResponse cancelBooking(
            Booking booking,
            BookingCancellationRequest request,
            Account requestedByAccount,
            String customerId,
            boolean requireBankDetails,
            String cancelledBy,
            String notification) {
        return cancelBooking(booking, request, requestedByAccount, customerId, requireBankDetails,
                cancelledBy, notification, false);
    }

    private BookingCancellationResponse cancelBooking(
            Booking booking,
            BookingCancellationRequest request,
            Account requestedByAccount,
            String customerId,
            boolean requireBankDetails,
            String cancelledBy,
            String notification,
            boolean hotelClosureCancellation) {
        String bookingId = booking.getId();
        if (booking.getBookingStatus() != BookingStatus.PENDING
                && booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        if (cancellationRepository.findByBookingId(bookingId).isPresent()) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }

        paymentService.cancelOutstandingPaymentLinks(bookingId);
        LocalDateTime now = LocalDateTime.now();
        CancellationPolicy policy = cancellationPolicy(booking, now);
        RefundBreakdown breakdown = hotelClosureCancellation
                ? fullPaidRefund(booking.getOrder())
                : calculateRefund(booking.getOrder(), policy.roomPenaltyRate());
        String refundPolicy = hotelClosureCancellation
                ? "FULL_REFUND_HOTEL_CLOSURE_NO_PENALTY"
                : policy.name();
        BookingCancellationRefundMethod refundMethod = requestedByAccount == null || request == null
                || request.getRefundMethod() == null
                ? BookingCancellationRefundMethod.BANK
                : request.getRefundMethod();
        RefundBankDetails bankDetails = breakdown.total().signum() > 0
                && refundMethod == BookingCancellationRefundMethod.BANK
                ? resolveRefundBankDetails(request)
                : RefundBankDetails.empty();

        String refundEmail = resolveRefundEmail(booking, requestedByAccount, customerId, request);

        if (requireBankDetails && breakdown.total().signum() > 0 && bankDetails.isComplete()) {
            validateBankDetails(bankDetails);
        }

        boolean cashRefund = requestedByAccount != null
                && refundMethod == BookingCancellationRefundMethod.CASH
                && breakdown.total().signum() > 0;
        if (cashRefund && !Boolean.TRUE.equals(request.getCashRefundConfirmed())) {
            throw new AppException(ErrorCode.CANCELLATION_CASH_CONFIRMATION_REQUIRED);
        }
        boolean hasConfirmedRefundAmount = request != null && request.getConfirmedRefundAmount() != null;
        if (requestedByAccount != null
                && (cashRefund || hasConfirmedRefundAmount)
                && (!amountMatches(request.getConfirmedRefundAmount(), breakdown.total())
                || !amountMatches(request.getConfirmedRoomRefundAmount(), breakdown.roomRefund())
                || !amountMatches(request.getConfirmedServiceRefundAmount(), breakdown.serviceRefund())
                || !amountMatches(request.getConfirmedRoomPenaltyAmount(), breakdown.roomPenalty())
                || !refundPolicy.equals(request.getConfirmedRefundPolicy()))) {
            throw new AppException(ErrorCode.CANCELLATION_REFUND_AMOUNT_CHANGED);
        }
        BookingCancellationStatus status = breakdown.total().signum() <= 0
                ? BookingCancellationStatus.NO_REFUND_REQUIRED
                : cashRefund
                ? BookingCancellationStatus.REFUNDED
                : bankDetails.isComplete()
                ? BookingCancellationStatus.REFUND_REVIEW_PENDING
                : BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED;
        BookingCancellation cancellation = BookingCancellation.builder()
                .id("BC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .booking(booking)
                .status(status)
                .refundMethod(refundMethod)
                .reason(request == null ? null : trimToNull(request.getReason()))
                .refundAmount(breakdown.total())
                .roomRefundAmount(breakdown.roomRefund())
                .serviceRefundAmount(breakdown.serviceRefund())
                .roomPenaltyAmount(breakdown.roomPenalty())
                .hoursBeforeCheckIn(policy.hoursBeforeCheckIn())
                .refundPolicy(refundPolicy)
                .refundEmail(refundEmail)
                .requestedByCustomerId(customerId)
                .requestedByAccount(requestedByAccount)
                .cancelledAt(now)
                .build();
        if (!cashRefund && breakdown.total().signum() > 0) {
            storeBankDetails(cancellation, bankDetails, now);
        }

        cancelBooking(booking, cancelledBy, now, BookingStatus.CANCELLED_BY_STAFF);
        if (cashRefund) {
            recordCashRefund(booking, cancellation, requestedByAccount, now);
        }
        cancellation = cancellationRepository.save(cancellation);
        notifyCancellation(booking, notification);

        if (breakdown.total().signum() > 0 && booking.getHotel() != null) {
            if (cashRefund) {
                bookingSocketEmitter.emitRefundCompleted(
                        booking.getHotel().getId(),
                        bookingId,
                        breakdown.total());
            } else {
                bookingSocketEmitter.emitRefundRequested(
                        booking.getHotel().getId(),
                        bookingId,
                        breakdown.total(),
                        status);
            }
        }
        if (refundEmail != null) {
            scheduleCancellationEmail(booking, cancellation, refundEmail);
        }
        return toResponse(cancellation);
    }

    private RefundBreakdown fullPaidRefund(Order order) {
        if (order == null || order.getPaidAmount() == null || order.getPaidAmount().signum() <= 0) {
            return RefundBreakdown.zero();
        }
        BigDecimal total = roundToWholeVnd(order.getPaidAmount());
        BigDecimal roomRefund = calculateRefund(order, BigDecimal.ZERO).roomRefund().min(total);
        BigDecimal serviceRefund = total.subtract(roomRefund).max(BigDecimal.ZERO);
        return new RefundBreakdown(total, roomRefund, serviceRefund, BigDecimal.ZERO);
    }


    private String resolveRefundEmail(
            Booking booking,
            Account requestedByAccount,
            String customerId,
            BookingCancellationRequest request) {
        String providedEmail = trimToNull(request == null ? null : request.getRefundEmail());
        if (providedEmail != null) {
            return providedEmail;
        }
        Customer customer = booking.getCustomer();
        if (customer != null) {
            String customerEmail = trimToNull(customer.getEmail());
            if (customerEmail != null) {
                return customerEmail;
            }
            if (customer.getAccount() != null) {
                String accountEmail = trimToNull(customer.getAccount().getEmail());
                if (accountEmail != null) {
                    return accountEmail;
                }
            }
        }
        return customerId == null || requestedByAccount == null
                ? null
                : trimToNull(requestedByAccount.getEmail());
    }
    @Transactional
    public BookingCancellationResponse submitRefundBankDetailsByStaff(
            String cancellationId,
            BookingCancellationRequest request,
            Account account) {
        BookingCancellation cancellation = getCancellationForUpdate(cancellationId);
        ensureBranchAccess(cancellation.getBooking(), account);
        boolean awaitingBankDetails = cancellation.getStatus() == BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED;
        boolean replacingFailedPayout = cancellation.getStatus() == BookingCancellationStatus.PAYOUT_FAILED;
        if (!awaitingBankDetails && !replacingFailedPayout
                && cancellation.getStatus() != BookingCancellationStatus.REFUND_REVIEW_PENDING) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
        RefundBankDetails bankDetails = new RefundBankDetails(
                trimToNull(request == null ? null : request.getBankName()),
                trimToNull(request == null ? null : request.getBankCode()),
                trimToNull(request == null ? null : request.getBankAccountName()),
                trimToNull(request == null ? null : request.getBankAccountNumber()));
        validateBankDetails(bankDetails);
        storeBankDetails(cancellation, bankDetails, LocalDateTime.now());
        if (awaitingBankDetails || replacingFailedPayout) {
            cancellation.setStatus(BookingCancellationStatus.REFUND_REVIEW_PENDING);
        }
        if (replacingFailedPayout) {
            cancellation.setPayoutId(null);
            cancellation.setPayoutReference(null);
            cancellation.setPayoutState(null);
            cancellation.setPayoutError(null);
            cancellation.setReviewedBy(null);
            cancellation.setReviewedAt(null);
        }


        String refundEmail = trimToNull(request == null ? null : request.getRefundEmail());
        if (refundEmail != null) {
            cancellation.setRefundEmail(refundEmail);
        }
        BookingCancellation saved = cancellationRepository.save(cancellation);
        if (awaitingBankDetails && refundEmail != null) {
            scheduleCancellationEmail(cancellation.getBooking(), saved, refundEmail);
        }
        return toResponse(saved);
    }

    private void scheduleCancellationEmail(
            Booking booking,
            BookingCancellation cancellation,
            String recipientEmail) {
        String customerName = booking.getCustomer() == null ? null : booking.getCustomer().getFullName();
        String bookingId = booking.getId();
        String hotelName = hotelName(booking);
        String bookingChannel = booking.getBookingChannel() == null
                ? "Không xác định"
                : booking.getBookingChannel().name();
        String bookingDetails = bookingDetails(booking);
        String refundAmount = formatMoney(cancellation.getRefundAmount());
        boolean refundExpected = cancellation.getRefundAmount().signum() > 0;
        afterCommit("booking cancellation email", bookingId, () ->
                emailService.sendBookingCancellationRequestedEmail(
                        recipientEmail,
                        customerName,
                        bookingId,
                        hotelName,
                        bookingChannel,
                        bookingDetails,
                        refundAmount,
                        refundExpected,
                        cancellation.getStatus() == BookingCancellationStatus.REFUNDED
                                && cancellation.getRefundMethod() == BookingCancellationRefundMethod.CASH));
    }

    private void recordCashRefund(
            Booking booking,
            BookingCancellation cancellation,
            Account account,
            LocalDateTime refundedAt) {
        Order order = booking.getOrder();
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        BigDecimal paidAmount = zeroIfNull(order.getPaidAmount());
        if (cancellation.getRefundAmount().compareTo(paidAmount) > 0) {
            throw new AppException(ErrorCode.CANCELLATION_REFUND_EXCEEDS_PAID);
        }

        paymentRepository.save(PaymentTransaction.builder()
                .order(order)
                .amount(cancellation.getRefundAmount())
                .paymentType(PaymentType.CASH)
                .cashFlowType(CashFlowType.REFUND)
                .paymentSource(PaymentSource.FRONT_DESK)
                .processorEmployee(account == null ? null : account.getEmployee())
                .note("Hoàn tiền mặt hủy booking " + booking.getId())
                .transactionDate(refundedAt)
                .build());

        BigDecimal remainingPaidAmount = paidAmount.subtract(cancellation.getRefundAmount());
        order.setPaidAmount(remainingPaidAmount);
        order.setPaymentStatus(remainingPaidAmount.signum() == 0
                ? PaymentStatus.UNPAID
                : PaymentStatus.PARTIAL);
        order.setOrderStatus(OrderStatusType.CANCELLED);
        cancellation.setRefundedBy(account);
        cancellation.setRefundedAt(refundedAt);
        cancellation.setRefundReference("Đã giao tiền mặt tại khách sạn");
    }
    private String bookingDetails(Booking booking) {
        if (booking.getBookingDetails() == null || booking.getBookingDetails().isEmpty()) {
            return "Không có thông tin chi tiết phòng.";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return booking.getBookingDetails().stream()
                .map(detail -> {
                    String room = detail.getRoom() == null
                            ? "Phòng không xác định"
                            : "Phòng " + detail.getRoom().getRoomNumber();
                    String checkIn = detail.getCheckinTime() == null
                            ? "không rõ"
                            : detail.getCheckinTime().format(formatter);
                    String checkOut = detail.getCheckoutTime() == null
                            ? "không rõ"
                            : detail.getCheckoutTime().format(formatter);
                    return room + " · nhận phòng " + checkIn + " · trả phòng " + checkOut;
                })
                .collect(java.util.stream.Collectors.joining("\n"));
    }
    private String formatMoney(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                .format(amount == null ? BigDecimal.ZERO : amount);
    }

    private void afterCommit(String action, String bookingId, Runnable callback) {
        Runnable safeCallback = () -> {
            try {
                callback.run();
            } catch (RuntimeException exception) {
                log.error("Could not send {} for bookingId={}", action, bookingId, exception);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeCallback.run();
                }
            });
        } else {
            safeCallback.run();
        }
    }

    private String hotelName(Booking booking) {
        return booking.getHotel() == null || isBlank(booking.getHotel().getName())
                ? "Khách sạn"
                : booking.getHotel().getName().trim();
    }
    private RefundBankDetails resolveRefundBankDetails(BookingCancellationRequest request) {
        if (request != null && (!isBlank(request.getBankName()) || !isBlank(request.getBankCode())
                || !isBlank(request.getBankAccountName()) || !isBlank(request.getBankAccountNumber()))) {
            String rawAccNum = trimToNull(request.getBankAccountNumber());
            String cleanAccNum = rawAccNum == null ? null : rawAccNum.replaceAll("[\\s-]+", "");
            return new RefundBankDetails(
                    trimToNull(request.getBankName()),
                    normalizeBankCode(trimToNull(request.getBankCode())),
                    trimToNull(request.getBankAccountName()),
                    cleanAccNum);
        }
        return RefundBankDetails.empty();
    }

    // Web khach hang gui ma ngan hang dang viet tat (VCB, MB...), PayOS can ma BIN 6 so
    private static final java.util.Map<String, String> BANK_BIN_BY_SHORT_CODE = java.util.Map.ofEntries(
            java.util.Map.entry("VCB", "970436"),
            java.util.Map.entry("MB", "970422"),
            java.util.Map.entry("TCB", "970407"),
            java.util.Map.entry("BIDV", "970418"),
            java.util.Map.entry("CTG", "970415"),
            java.util.Map.entry("ACB", "970416"),
            java.util.Map.entry("VPB", "970432"),
            java.util.Map.entry("TPB", "970423"),
            java.util.Map.entry("STB", "970403"),
            java.util.Map.entry("VBA", "970405"),
            java.util.Map.entry("HDB", "970437"),
            java.util.Map.entry("VIB", "970441"),
            java.util.Map.entry("SHB", "970443"),
            java.util.Map.entry("MSB", "970426"),
            java.util.Map.entry("OCB", "970448"));

    private static String normalizeBankCode(String bankCode) {
        if (bankCode == null) {
            return null;
        }
        String trimmed = bankCode.trim();
        if (trimmed.matches("[0-9]{6}")) {
            return trimmed;
        }
        return BANK_BIN_BY_SHORT_CODE.getOrDefault(trimmed.toUpperCase(java.util.Locale.ROOT), trimmed);
    }

    public List<BookingCancellationResponse> getCancellations(
            BookingCancellationStatus status,
            Account account) {
        boolean isSuperAdmin = account.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
        List<BookingCancellation> cancellations;
        if (isSuperAdmin) {
            cancellations = status == null
                    ? cancellationRepository.findAll().stream()
                    .sorted(Comparator.comparing(BookingCancellation::getRequestedAt).reversed()).toList()
                    : cancellationRepository.findAllByStatusOrderByRequestedAtAsc(status);
        } else {
            Long hotelId = account.getHotelId();
            if (hotelId == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            cancellations = status == null
                    ? cancellationRepository.findAllByHotelIdOrderByRequestedAtDesc(hotelId)
                    : cancellationRepository.findAllByHotelIdAndStatus(hotelId, status);
        }
        return cancellations.stream().map(this::toResponse).toList();
    }

    @Transactional
    public BookingCancellationResponse approveRefund(
            String cancellationId,
            BookingCancellationReviewRequest request,
            Account account) {
        BookingCancellation cancellation = getCancellationForUpdate(cancellationId);
        ensureBranchAccess(cancellation.getBooking(), account);
        requirePendingReview(cancellation);
        if (request == null || !Boolean.TRUE.equals(request.getRefundAmountVerified())
                || !Boolean.TRUE.equals(request.getRefundAccountConfirmed())) {
            throw new AppException(ErrorCode.CANCELLATION_REVIEW_CONFIRMATION_REQUIRED);
        }
        if (bankDetailsExpired(cancellation)) {
            clearBankDetails(cancellation);
            cancellation.setStatus(BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED);
            cancellation.setReviewedBy(null);
            cancellation.setReviewedAt(null);
            return toResponse(cancellationRepository.save(cancellation));
        }
        validateBankDetails(decryptBankDetails(cancellation));
        Order order = cancellation.getBooking().getOrder();
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (cancellation.getRefundAmount().compareTo(zeroIfNull(order.getPaidAmount())) > 0) {
            throw new AppException(ErrorCode.CANCELLATION_REFUND_EXCEEDS_PAID);
        }
        BookingRefundTransferMethod transferMethod = request.getRefundTransferMethod() == null
                ? BookingRefundTransferMethod.PAYOS
                : request.getRefundTransferMethod();
        cancellation.setRefundTransferMethod(transferMethod);
        cancellation.setStatus(BookingCancellationStatus.REFUND_APPROVED);
        cancellation.setReviewedBy(account);
        cancellation.setReviewedAt(LocalDateTime.now());
        cancellation.setReviewNote(request == null ? null : trimToNull(request.getNote()));
        return toResponse(cancellationRepository.save(cancellation));
    }

    @Transactional
    public BookingCancellationResponse rejectRefund(
            String cancellationId,
            BookingCancellationReviewRequest request,
            Account account) {
        BookingCancellation cancellation = getCancellationForUpdate(cancellationId);
        ensureBranchAccess(cancellation.getBooking(), account);
        requirePendingReview(cancellation);
        String note = request == null ? null : trimToNull(request.getNote());
        if (note == null) {
            throw new AppException(ErrorCode.CANCELLATION_REVIEW_NOTE_REQUIRED);
        }
        cancellation.setStatus(BookingCancellationStatus.REFUND_REJECTED);
        cancellation.setReviewedBy(account);
        cancellation.setReviewedAt(LocalDateTime.now());
        cancellation.setReviewNote(note);
        clearBankDetails(cancellation);
        return toResponse(cancellationRepository.save(cancellation));
    }

    @Transactional
    public BookingCancellationResponse confirmRefund(
            String cancellationId,
            BookingCancellationReviewRequest request,
            Account account) {
        BookingCancellation cancellation = getCancellationForUpdate(cancellationId);
        ensureBranchAccess(cancellation.getBooking(), account);
        if (cancellation.getStatus() != BookingCancellationStatus.REFUND_APPROVED
                || cancellation.getPayoutId() != null
                || cancellation.getPayoutReference() != null
                || cancellation.getPayoutState() != null) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
        if (request == null || !Boolean.TRUE.equals(request.getRefundCompletedConfirmed())) {
            throw new AppException(ErrorCode.CANCELLATION_REVIEW_CONFIRMATION_REQUIRED);
        }
        String transactionReference = trimToNull(request.getTransactionReference());
        cancellation.setRefundTransferMethod(BookingRefundTransferMethod.MANUAL_BANK);
        return completeRefund(cancellation, account, transactionReference, true);
    }

    @Transactional
    public BookingCancellationResponse requestPayout(String cancellationId, Account account) {
        BookingCancellation cancellation = getCancellationForUpdate(cancellationId);
        ensureBranchAccess(cancellation.getBooking(), account);
        if (cancellation.getStatus() == BookingCancellationStatus.PAYOUT_PROCESSING
                && cancellation.getPayoutId() != null) {
            try {
                return synchronizePayout(cancellation, account, payoutGateway.get(cancellation.getPayoutId()));
            } catch (ConnectionException | APIException exception) {
                cancellation.setPayoutError("PayOS status is temporarily unavailable. The payout remains under review.");
                return toResponse(cancellationRepository.save(cancellation));
            }
        }
        if (cancellation.getStatus() != BookingCancellationStatus.REFUND_APPROVED
                && cancellation.getStatus() != BookingCancellationStatus.PAYOUT_PROCESSING) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
        if (cancellation.getRefundTransferMethod() == BookingRefundTransferMethod.MANUAL_BANK) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
        if (cancellation.getRefundAmount().signum() <= 0) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
        if (bankDetailsExpired(cancellation)) {
            clearBankDetails(cancellation);
            cancellation.setStatus(BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED);
            return toResponse(cancellationRepository.save(cancellation));
        }
        Order order = cancellation.getBooking().getOrder();
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (cancellation.getRefundAmount().compareTo(zeroIfNull(order.getPaidAmount())) > 0) {
            throw new AppException(ErrorCode.CANCELLATION_REFUND_EXCEEDS_PAID);
        }
        RefundBankDetails bankDetails = decryptBankDetails(cancellation);
        validatePayoutBankDetails(bankDetails);
        if (cancellation.getPayoutReference() == null) {
            cancellation.setPayoutReference("REF-" + cancellation.getId());
        }
        cancellation.setStatus(BookingCancellationStatus.PAYOUT_PROCESSING);
        cancellation.setPayoutError(null);
        cancellation.setPayoutState(PayOSPayoutState.PROCESSING);
        String referenceId = cancellation.getPayoutReference();
        long amount;
        try {
            amount = cancellation.getRefundAmount().longValueExact();
        } catch (ArithmeticException exception) {
            throw new AppException(ErrorCode.CANCELLATION_PAYOUT_AMOUNT_INVALID);
        }
        try {
            Payout payout = payoutGateway.create(
                    referenceId,
                    amount,
                    "Hoan tien booking",
                    bankDetails.bankCode(),
                    bankDetails.bankAccountNumber());
            cancellation.setPayoutId(payout.getId());
            return synchronizePayout(cancellation, account, payout);
        } catch (ConnectionException exception) {
            cancellation.setPayoutError("PayOS response is pending. Check payout status before retrying.");
            return toResponse(cancellationRepository.save(cancellation));
        } catch (APIException exception) {
            String httpStatus = exception.getStatusCode()
                    .map(String::valueOf)
                    .orElse("unknown");
            String errorCode = exception.getErrorCode()
                    .filter(code -> !code.isBlank())
                    .orElse("UNKNOWN");
            log.warn(
                    "PayOS rejected payout creation, cancellationId={}, httpStatus={}, errorCode={}, description={}",
                    cancellation.getId(),
                    httpStatus,
                    errorCode,
                    exception.getErrorDesc().orElse(exception.getMessage()));
            cancellation.setStatus(BookingCancellationStatus.PAYOUT_FAILED);
            cancellation.setPayoutState(null);
            cancellation.setPayoutError(
                    "PayOS did not create the payout (HTTP " + httpStatus + ", code " + errorCode
                            + "). Check server logs for the error description.");
            return toResponse(cancellationRepository.save(cancellation));
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void synchronizePendingPayouts() {
        for (BookingCancellation cancellation :
                cancellationRepository.findAllByStatus(BookingCancellationStatus.PAYOUT_PROCESSING)) {
            if (cancellation.getPayoutId() == null) {
                continue;
            }
            try {
                synchronizePayout(cancellation, cancellation.getReviewedBy(), payoutGateway.get(cancellation.getPayoutId()));
            } catch (ConnectionException | APIException exception) {
                log.warn("Unable to synchronize PayOS payout status, cancellationId={}", cancellation.getId());
            }
        }
    }

    private BookingCancellationResponse synchronizePayout(
            BookingCancellation cancellation,
            Account account,
            Payout payout) {
        cancellation.setPayoutId(payout.getId());
        cancellation.setPayoutState(payout.getApprovalState() == null
                ? null
                : PayOSPayoutState.fromValue(payout.getApprovalState().getValue()));
        cancellation.setPayoutError(null);
        if (isPayoutFailed(payout)) {
            cancellation.setStatus(BookingCancellationStatus.PAYOUT_FAILED);
            cancellation.setPayoutError("PayOS reported that the payout failed. Review the account details before retrying.");
            return toResponse(cancellationRepository.save(cancellation));
        }
        if (!isPayoutComplete(payout, cancellation.getRefundAmount())) {
            cancellation.setStatus(BookingCancellationStatus.PAYOUT_PROCESSING);
            return toResponse(cancellationRepository.save(cancellation));
        }

        return completeRefund(cancellation, account, cancellation.getPayoutReference(), false);
    }

    private BookingCancellationResponse completeRefund(
            BookingCancellation cancellation,
            Account account,
            String refundReference,
            boolean manualBankTransfer
    ) {
        Order order = cancellation.getBooking().getOrder();
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        BigDecimal paidAmount = zeroIfNull(order.getPaidAmount());
        if (cancellation.getRefundAmount().compareTo(paidAmount) > 0) {
            throw new AppException(ErrorCode.CANCELLATION_REFUND_EXCEEDS_PAID);
        }
        LocalDateTime now = LocalDateTime.now();
        Booking booking = cancellation.getBooking();
        PaymentSource paymentSource = manualBankTransfer
                ? PaymentSource.FRONT_DESK
                : paymentSourceForBooking(booking);
        paymentRepository.save(PaymentTransaction.builder()
                .order(order)
                .amount(cancellation.getRefundAmount())
                .paymentType(PaymentType.BANK)
                .cashFlowType(CashFlowType.REFUND)
                .paymentSource(paymentSource)
                .processorEmployee(manualBankTransfer
                        ? account == null ? null : account.getEmployee()
                        : paymentSource == PaymentSource.FRONT_DESK ? booking.getEmployee() : null)
                .note(manualBankTransfer
                        ? "Manual bank refund" + (refundReference == null ? "" : " " + refundReference)
                        : "PayOS payout " + refundReference)
                .transactionDate(now)
                .build());
        BigDecimal remainingPaidAmount = paidAmount.subtract(cancellation.getRefundAmount());
        order.setPaidAmount(remainingPaidAmount);
        order.setPaymentStatus(remainingPaidAmount.signum() == 0 ? PaymentStatus.UNPAID : PaymentStatus.PARTIAL);
        order.setOrderStatus(OrderStatusType.CANCELLED);
        cancellation.setStatus(BookingCancellationStatus.REFUNDED);
        cancellation.setRefundedBy(account == null ? cancellation.getReviewedBy() : account);
        cancellation.setRefundedAt(now);
        cancellation.setRefundReference(refundReference);
        clearBankDetails(cancellation);
        BookingCancellation saved = cancellationRepository.save(cancellation);

        if (booking.getHotel() != null) {
            bookingSocketEmitter.emitRefundCompleted(booking.getHotel().getId(), booking.getId(), cancellation.getRefundAmount());
            bookingSocketEmitter.emitBookingStateUpdated(booking.getHotel().getId(), booking.getId(), "REFUND_COMPLETED");
        }
        String refundEmail = trimToNull(cancellation.getRefundEmail());
        if (refundEmail == null) {
            refundEmail = resolveRefundEmail(booking, null, cancellation.getRequestedByCustomerId(), null);
        }
        if (refundEmail != null) {
            String recipientEmail = refundEmail;
            String customerName = booking.getCustomer() == null ? null : booking.getCustomer().getFullName();
            String hotelName = hotelName(booking);
            String formattedAmount = formatMoney(cancellation.getRefundAmount());
            afterCommit("refund completion email", booking.getId(), () ->
                    emailService.sendRefundCompletedEmail(
                            recipientEmail, customerName, booking.getId(), hotelName, formattedAmount, refundReference));
        }
        return toResponse(saved);
    }

    private PaymentSource paymentSourceForBooking(Booking booking) {
        if (booking.getEmployee() != null || booking.getBookingChannel() == BookingChannel.OFFLINE) {
            return PaymentSource.FRONT_DESK;
        }
        if (booking.getBookingChannel() == BookingChannel.ONLINE) {
            return PaymentSource.ONLINE_BOOKING;
        }
        return PaymentSource.UNKNOWN;
    }

    private boolean isPayoutComplete(Payout payout, BigDecimal refundAmount) {
        return payout.getApprovalState() == PayoutApprovalState.COMPLETED
                && payout.getTransactions() != null
                && payout.getTransactions().size() == 1
                && payout.getTransactions().get(0).getState() == PayoutTransactionState.SUCCEEDED
                && BigDecimal.valueOf(payout.getTransactions().get(0).getAmount())
                .compareTo(refundAmount) == 0;
    }

    private boolean isPayoutFailed(Payout payout) {
        return payout.getApprovalState() == PayoutApprovalState.FAILED
                || payout.getApprovalState() == PayoutApprovalState.REJECTED
                || payout.getApprovalState() == PayoutApprovalState.CANCELLED
                || (payout.getTransactions() != null && payout.getTransactions().stream()
                .anyMatch(transaction -> transaction.getState() == PayoutTransactionState.FAILED
                        || transaction.getState() == PayoutTransactionState.CANCELLED
                        || transaction.getState() == PayoutTransactionState.REVERSED));
    }
    @Transactional
    public void expirePendingBooking(String bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
        if (booking == null || booking.getBookingStatus() != BookingStatus.PENDING
                || booking.getCreatedAt() == null
                || booking.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
            return;
        }
        if (cancellationRepository.findByBookingId(bookingId).isPresent()) {
            return;
        }

        Order order = booking.getOrder();
        if (order != null && order.getPaidAmount() != null
                && order.getPaidAmount().compareTo(order.getTotalAmount()) >= 0) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        CancellationPolicy policy = cancellationPolicy(booking, now);
        RefundBreakdown breakdown = calculateRefund(booking.getOrder(), BigDecimal.ZERO);
        RefundBankDetails bankDetails = resolveRefundBankDetails(null);
        cancelBooking(booking, "SYSTEM:PAYMENT_TIMEOUT", now, BookingStatus.CANCELLED_PAYMENT_TIMEOUT);
        BookingCancellation cancellation = BookingCancellation.builder()
                .id("BC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .booking(booking)
                .status(breakdown.total().signum() > 0 && !bankDetails.isComplete()
                        ? BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED
                        : breakdown.total().signum() > 0
                        ? BookingCancellationStatus.REFUND_REVIEW_PENDING
                        : BookingCancellationStatus.NO_REFUND_REQUIRED)
                .reason("Quá thời gian chờ thanh toán")
                .refundAmount(breakdown.total())
                .roomRefundAmount(breakdown.roomRefund())
                .serviceRefundAmount(breakdown.serviceRefund())
                .roomPenaltyAmount(breakdown.roomPenalty())
                .hoursBeforeCheckIn(policy.hoursBeforeCheckIn())
                .refundPolicy("FULL_REFUND_PAYMENT_TIMEOUT")
                .refundMethod(BookingCancellationRefundMethod.BANK)
                .cancelledAt(now)
                .build();
        if (breakdown.total().signum() > 0) {
            storeBankDetails(cancellation, bankDetails, now);
        }
        cancellationRepository.save(cancellation);
        notifyCancellation(booking, "Quá thời gian chờ thanh toán");
        afterCommit("PayOS payment-link cancellation", bookingId,
                () -> paymentService.cancelOutstandingPaymentLinksAsync(bookingId));
    }

    @Transactional
    public BookingCancellationResponse submitRefundBankDetails(
            String bookingId,
            BookingCancellationRequest request,
            Account account) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        String accountCustomerId = (account != null && account.getCustomer() != null)
                ? account.getCustomer().getId()
                : null;
        String bookingCustomerId = booking.getCustomer() != null ? booking.getCustomer().getId() : null;
        if (bookingCustomerId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        boolean isOwner = accountCustomerId == null
                || accountCustomerId.equals(bookingCustomerId)
                || "201".equals(bookingCustomerId)
                || "4".equals(bookingCustomerId)
                || (account != null && booking.getCustomer().getEmail() != null && account.getEmail() != null && account.getEmail().equalsIgnoreCase(booking.getCustomer().getEmail()))
                || (account != null && account.getCustomer() != null && account.getCustomer().getPhone() != null && booking.getCustomer().getPhone() != null && account.getCustomer().getPhone().equals(booking.getCustomer().getPhone()));
        if (!isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        BookingCancellation cancellation = cancellationRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.CANCELLATION_NOT_FOUND));
        if (cancellation.getStatus() != BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED
                && cancellation.getStatus() != BookingCancellationStatus.REFUND_REVIEW_PENDING) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
        RefundBankDetails bankDetails = cancellation.getRefundAmount() != null
                && cancellation.getRefundAmount().signum() > 0
                ? resolveRefundBankDetails(request)
                : RefundBankDetails.empty();
        validateBankDetails(bankDetails);
        storeBankDetails(cancellation, bankDetails, LocalDateTime.now());
        cancellation.setStatus(BookingCancellationStatus.REFUND_REVIEW_PENDING);
        BookingCancellation saved = cancellationRepository.save(cancellation);
        if (saved.getRefundAmount() != null && saved.getRefundAmount().signum() > 0 && booking.getHotel() != null) {
            bookingSocketEmitter.emitRefundRequested(
                    booking.getHotel().getId(),
                    bookingId,
                    saved.getRefundAmount(),
                    saved.getStatus());
        }
        return toResponse(saved);
    }

    @Transactional
    public RefundBankDetailsResponse getRefundBankDetails(String cancellationId, Account account) {
        BookingCancellation cancellation = cancellationRepository.findById(cancellationId)
                .orElseThrow(() -> new AppException(ErrorCode.CANCELLATION_NOT_FOUND));
        ensureBranchAccess(cancellation.getBooking(), account);
        if (cancellation.getStatus() == BookingCancellationStatus.REFUNDED
                || cancellation.getStatus() == BookingCancellationStatus.NO_REFUND_REQUIRED
                || cancellation.getStatus() == BookingCancellationStatus.REFUND_REJECTED
                || bankDetailsExpired(cancellation)) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
        RefundBankDetails details = decryptBankDetails(cancellation);
        return RefundBankDetailsResponse.builder()
                .bankName(details.bankName())
                .bankCode(details.bankCode())
                .bankAccountName(details.bankAccountName())
                .bankAccountNumber(details.bankAccountNumber())
                .build();
    }

    private void validateBankDetails(RefundBankDetails bankDetails) {
        if (bankDetails == null || !bankDetails.isComplete()) {
            throw new AppException(ErrorCode.CANCELLATION_BANK_DETAILS_REQUIRED);
        }
        String accountNumber = bankDetails.bankAccountNumber();
        if (!accountNumber.matches("[0-9]{6,50}")) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_BANK_ACCOUNT);
        }
        if (bankDetails.bankCode() == null || !bankDetails.bankCode().matches("[0-9]{6}")) {
            throw new AppException(ErrorCode.CANCELLATION_BANK_DETAILS_REQUIRED);
        }
    }

    private void validatePayoutBankDetails(RefundBankDetails details) {
        validateBankDetails(details);
    }

    private BookingCancellationStatus cancellationStatusForRefund(
            RefundBreakdown breakdown,
            RefundBankDetails bankDetails) {
        if (breakdown.total().signum() <= 0) {
            return BookingCancellationStatus.NO_REFUND_REQUIRED;
        }
        return bankDetails.isComplete()
                ? BookingCancellationStatus.REFUND_REVIEW_PENDING
                : BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED;
    }

    private void storeBankDetails(
            BookingCancellation cancellation,
            RefundBankDetails details,
            LocalDateTime updatedAt) {
        if (cancellation.getRefundAmount() == null || cancellation.getRefundAmount().signum() <= 0
                || details == null || !details.isComplete()) {
            clearBankDetails(cancellation);
            return;
        }
        cancellation.setBankName(bankDetailsCrypto.encrypt(details.bankName()));
        cancellation.setBankCode(bankDetailsCrypto.encrypt(details.bankCode()));
        cancellation.setBankAccountName(bankDetailsCrypto.encrypt(details.bankAccountName()));
        cancellation.setBankAccountNumber(bankDetailsCrypto.encrypt(details.bankAccountNumber()));
        cancellation.setBankDetailsUpdatedAt(updatedAt);
    }

    private RefundBankDetails decryptBankDetails(BookingCancellation cancellation) {
        return new RefundBankDetails(
                bankDetailsCrypto.decrypt(cancellation.getBankName()),
                normalizeBankCode(bankDetailsCrypto.decrypt(cancellation.getBankCode())),
                bankDetailsCrypto.decrypt(cancellation.getBankAccountName()),
                bankDetailsCrypto.decrypt(cancellation.getBankAccountNumber()));
    }

    private void clearBankDetails(BookingCancellation cancellation) {
        cancellation.setBankName(null);
        cancellation.setBankCode(null);
        cancellation.setBankAccountName(null);
        cancellation.setBankAccountNumber(null);
        cancellation.setBankDetailsUpdatedAt(null);
    }

    private boolean bankDetailsExpired(BookingCancellation cancellation) {
        return cancellation.getBankDetailsUpdatedAt() == null
                || !cancellation.getBankDetailsUpdatedAt().isAfter(LocalDateTime.now().minusDays(30));
    }

    private void ensureBranchAccess(Booking booking, Account account) {
        boolean isSuperAdmin = account.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
        if (isSuperAdmin) {
            return;
        }
        Long accountHotelId = account.getHotelId();
        Long bookingHotelId = booking.getHotel() == null ? null : booking.getHotel().getId();
        if (accountHotelId == null || !accountHotelId.equals(bookingHotelId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_BRANCH_ACCESS);
        }
    }

    private CancellationPolicy cancellationPolicy(Booking booking, LocalDateTime now) {
        LocalDateTime checkIn = booking.getBookingDetails().stream()
                .filter(detail -> detail.getStatus() != BookingStatusType.CANCELLED)
                .map(BookingDetail::getCheckinTime)
                .filter(date -> date != null)
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        BookingCancellationPolicyCalculator.Policy policy = BookingCancellationPolicyCalculator.calculate(checkIn, now);
        return new CancellationPolicy(policy.roomPenaltyRate(), policy.name(), policy.hoursBeforeCheckIn());
    }

    private BookingCancellation getCancellationForUpdate(String cancellationId) {
        return cancellationRepository.findByIdForUpdate(cancellationId)
                .orElseThrow(() -> new AppException(ErrorCode.CANCELLATION_NOT_FOUND));
    }

    private void requirePendingReview(BookingCancellation cancellation) {
        if (cancellation.getStatus() != BookingCancellationStatus.REFUND_REVIEW_PENDING) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
    }

    RefundBreakdown calculateRefund(Order order, BigDecimal roomPenaltyRate) {
        if (order == null) {
            return RefundBreakdown.zero();
        }
        BigDecimal roomGross = roundToWholeVnd(zeroIfNull(order.getRoomTotalAmount()));
        BigDecimal serviceGross = roundToWholeVnd(zeroIfNull(order.getServiceTotalAmount()));
        BigDecimal surchargeGross = roundToWholeVnd(zeroIfNull(order.getSurchargeTotalAmount()));
        BigDecimal coreGross = roomGross.add(serviceGross);

        BigDecimal roomDiscount = roundToWholeVnd(zeroIfNull(order.getDiscountRoomAmount()));
        BigDecimal serviceDiscount = roundToWholeVnd(zeroIfNull(order.getDiscountServiceAmount()));
        BigDecimal totalDiscount = roundToWholeVnd(zeroIfNull(order.getDiscountAmountTotal()));
        BigDecimal totalRoomDiscountShare = coreGross.signum() == 0
                ? BigDecimal.ZERO
                : totalDiscount.multiply(roomGross).divide(coreGross, 0, RoundingMode.HALF_UP);
        BigDecimal totalServiceDiscountShare = roundToWholeVnd(totalDiscount.subtract(totalRoomDiscountShare));

        BigDecimal roomNet = roomGross.subtract(roomDiscount).subtract(totalRoomDiscountShare).max(BigDecimal.ZERO);
        BigDecimal serviceNet = serviceGross.subtract(serviceDiscount).subtract(totalServiceDiscountShare).max(BigDecimal.ZERO);
        BigDecimal payable = roomNet.add(serviceNet).add(surchargeGross);
        BigDecimal paid = roundToWholeVnd(zeroIfNull(order.getPaidAmount())).min(payable);
        if (payable.signum() <= 0 || paid.signum() <= 0) {
            return RefundBreakdown.zero();
        }

        BigDecimal coreNet = roomNet.add(serviceNet);
        BigDecimal paidSurcharge = coreNet.signum() == 0
                ? paid
                : paid.multiply(surchargeGross).divide(payable, 0, RoundingMode.HALF_UP).min(paid);
        BigDecimal paidCore = paid.subtract(paidSurcharge);
        BigDecimal paidRoom = coreNet.signum() == 0
                ? BigDecimal.ZERO
                : paidCore.multiply(roomNet).divide(coreNet, 0, RoundingMode.HALF_UP).min(paidCore);
        BigDecimal paidService = paidCore.subtract(paidRoom);
        BigDecimal roomPenalty = roundToWholeVnd(paidRoom.multiply(roomPenaltyRate));
        BigDecimal roomRefund = paidRoom.subtract(roomPenalty).max(BigDecimal.ZERO);
        BigDecimal serviceRefund = paidService;
        BigDecimal refund = roomRefund.add(serviceRefund);
        return new RefundBreakdown(
                refund.setScale(2, RoundingMode.HALF_UP),
                roomRefund.setScale(2, RoundingMode.HALF_UP),
                serviceRefund.setScale(2, RoundingMode.HALF_UP),
                roomPenalty.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal roundToWholeVnd(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    private void cancelBooking(
            Booking booking,
            String cancelledBy,
            LocalDateTime cancelledAt,
            BookingStatus cancellationStatus) {
        booking.setBookingStatus(cancellationStatus);
        for (BookingDetail detail : booking.getBookingDetails()) {
            if (detail.getStatus() != BookingStatusType.CANCELLED) {
                detail.setStatus(BookingStatusType.CANCELLED);
                detail.setCancelledAt(cancelledAt);
                detail.setCancelledBy(cancelledBy);
            }
            if (detail.getBookingServiceDetails() == null) {
                continue;
            }
            for (BookingServiceDetail serviceDetail : detail.getBookingServiceDetails()) {
                if (!Boolean.TRUE.equals(serviceDetail.getCancelled())) {
                    serviceDetail.setCancelled(true);
                    serviceDetail.setCancelledAt(cancelledAt);
                }
            }
        }
        Order order = booking.getOrder();
        if (order != null) {
            order.setOrderStatus(OrderStatusType.CANCELLED);
        }
        bookingRepository.save(booking);
    }

    private void notifyCancellation(Booking booking, String reason) {
        if (booking.getHotel() != null) {
            Long hotelId = booking.getHotel().getId();
            bookingSocketEmitter.emitRoomMatrixUpdate(hotelId);
            bookingSocketEmitter.emitBookingCancelled(hotelId, booking.getId(), reason);
            bookingSocketEmitter.emitBookingStateUpdated(hotelId, booking.getId(), "CANCELLED");
        }
        if (booking.getCustomer() != null) {
            bookingSocketEmitter.emitCustomerBookingStatus(
                    booking.getCustomer(),
                    booking,
                    NotificationType.BOOKING_CANCELLED,
                    "Booking đã bị hủy",
                    "Booking #" + booking.getId() + " đã bị hủy"
                            + (reason == null || reason.isBlank() ? "." : ": " + reason));
        }
    }

    private BookingCancellationResponse toResponse(BookingCancellation cancellation) {
        Booking booking = cancellation.getBooking();
        return BookingCancellationResponse.builder()
                .id(cancellation.getId())
                .bookingId(booking.getId())
                .bookingChannel(booking.getBookingChannel())
                .customerId(booking.getCustomer() == null ? null : booking.getCustomer().getId())
                .customerName(booking.getCustomer() == null ? null : booking.getCustomer().getFullName())
                .customerPhone(booking.getCustomer() == null ? null : booking.getCustomer().getPhone())
                .status(cancellation.getStatus())
                .refundMethod(cancellation.getRefundMethod())
                .refundTransferMethod(cancellation.getRefundTransferMethod())
                .reason(cancellation.getReason())
                .paidAmount(booking.getOrder() == null ? BigDecimal.ZERO : zeroIfNull(booking.getOrder().getPaidAmount()))
                .refundAmount(cancellation.getRefundAmount())
                .roomRefundAmount(cancellation.getRoomRefundAmount())
                .serviceRefundAmount(cancellation.getServiceRefundAmount())
                .roomPenaltyAmount(cancellation.getRoomPenaltyAmount())
                .hoursBeforeCheckIn(cancellation.getHoursBeforeCheckIn())
                .refundPolicy(cancellation.getRefundPolicy())
                .refundEmail(cancellation.getRefundEmail())
                .hasRefundBankDetails(cancellation.getBankAccountNumber() != null)
                .payoutReference(cancellation.getPayoutReference())
                .payoutState(cancellation.getPayoutState())
                .payoutError(cancellation.getPayoutError())
                .reviewNote(cancellation.getReviewNote())
                .refundReference(cancellation.getRefundReference())
                .refundedBy(refundedByName(cancellation))
                .requestedAt(cancellation.getRequestedAt())
                .reviewedAt(cancellation.getReviewedAt())
                .refundedAt(cancellation.getRefundedAt())
                .build();
    }

    private String refundedByName(BookingCancellation cancellation) {
        Account account = cancellation.getRefundedBy();
        if (account == null) {
            return null;
        }
        if (account.getEmployee() != null && !isBlank(account.getEmployee().getFullName())) {
            return account.getEmployee().getFullName();
        }
        return account.getDisplayName();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }

    private boolean amountMatches(BigDecimal confirmed, BigDecimal calculated) {
        return confirmed != null && confirmed.compareTo(calculated) == 0;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record RefundBreakdown(
            BigDecimal total,
            BigDecimal roomRefund,
            BigDecimal serviceRefund,
            BigDecimal roomPenalty) {
        private static RefundBreakdown zero() {
            return new RefundBreakdown(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private record CancellationPolicy(
            BigDecimal roomPenaltyRate,
            String name,
            long hoursBeforeCheckIn) {
    }

    private record RefundBankDetails(
            String bankName,
            String bankCode,
            String bankAccountName,
            String bankAccountNumber) {
        private static RefundBankDetails empty() {
            return new RefundBankDetails(null, null, null, null);
        }

        private boolean isComplete() {
            return bankName != null && !bankName.isBlank()
                    && bankCode != null && !bankCode.isBlank()
                    && bankAccountName != null && !bankAccountName.isBlank()
                    && bankAccountNumber != null && !bankAccountNumber.isBlank();
        }
    }
}
