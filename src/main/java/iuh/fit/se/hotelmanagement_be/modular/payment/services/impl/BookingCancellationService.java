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
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatusType;
import iuh.fit.se.hotelmanagement_be.modular.notification.entities.enums.NotificationType;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingCancellationRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.requests.bookingcancel.BookingCancellationReviewRequest;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.bookingcancel.BookingCancellationResponse;
import iuh.fit.se.hotelmanagement_be.modular.booking.services.impl.BookingSocketEmitter;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.PaymentTransaction;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.OrderStatusType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentStatus;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.services.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

    @Transactional
    public BookingCancellationResponse requestCancellation(
            String bookingId,
            BookingCancellationRequest request,
            Account account) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        String customerId = account.getCustomer() == null ? null : account.getCustomer().getId();
        if (customerId == null || booking.getCustomer() == null
                || !customerId.equals(booking.getCustomer().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
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
        RefundBankDetails bankDetails = resolveRefundBankDetails(booking.getOrder(), request);
        if (breakdown.total().signum() > 0) {
            validateBankDetails(bankDetails);
        }

        BookingCancellation cancellation = BookingCancellation.builder()
                .id("BC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .booking(booking)
                .status(breakdown.total().signum() > 0
                        ? BookingCancellationStatus.REFUND_REVIEW_PENDING
                        : BookingCancellationStatus.NO_REFUND_REQUIRED)
                .reason(request == null ? null : trimToNull(request.getReason()))
                .refundAmount(breakdown.total())
                .roomRefundAmount(breakdown.roomRefund())
                .serviceRefundAmount(breakdown.serviceRefund())
                .roomPenaltyAmount(breakdown.roomPenalty())
                .hoursBeforeCheckIn(policy.hoursBeforeCheckIn())
                .refundPolicy(policy.name())
                .bankName(bankDetails.bankName())
                .bankCode(bankDetails.bankCode())
                .bankAccountName(bankDetails.bankAccountName())
                .bankAccountNumber(bankDetails.bankAccountNumber())
                .requestedByCustomerId(customerId)
                .cancelledAt(now)
                .build();

        cancelBooking(booking, "CUSTOMER:" + customerId, now);
        cancellation = cancellationRepository.save(cancellation);
        notifyCancellation(booking, "Khách hàng yêu cầu hủy booking");
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

    private BookingCancellationResponse cancelBooking(
            Booking booking,
            BookingCancellationRequest request,
            Account requestedByAccount,
            String customerId,
            boolean requireBankDetails,
            String cancelledBy,
            String notification) {
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
        RefundBreakdown breakdown = calculateRefund(booking.getOrder(), policy.roomPenaltyRate());
        RefundBankDetails bankDetails = resolveRefundBankDetails(booking.getOrder(), request);

        String refundEmail = resolveRefundEmail(booking, requestedByAccount, customerId, request);

        if (requireBankDetails && breakdown.total().signum() > 0) {
            validateBankDetails(bankDetails);
        }

        BookingCancellationStatus status = breakdown.total().signum() <= 0
                ? BookingCancellationStatus.NO_REFUND_REQUIRED
                : bankDetails.isComplete()
                ? BookingCancellationStatus.REFUND_REVIEW_PENDING
                : BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED;
        BookingCancellation cancellation = BookingCancellation.builder()
                .id("BC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .booking(booking)
                .status(status)
                .reason(request == null ? null : trimToNull(request.getReason()))
                .refundAmount(breakdown.total())
                .roomRefundAmount(breakdown.roomRefund())
                .serviceRefundAmount(breakdown.serviceRefund())
                .roomPenaltyAmount(breakdown.roomPenalty())
                .hoursBeforeCheckIn(policy.hoursBeforeCheckIn())
                .refundPolicy(policy.name())
                .bankName(bankDetails.bankName())
                .bankCode(bankDetails.bankCode())
                .bankAccountName(bankDetails.bankAccountName())
                .bankAccountNumber(bankDetails.bankAccountNumber())
                .refundEmail(refundEmail)
                .requestedByCustomerId(customerId)
                .requestedByAccount(requestedByAccount)
                .cancelledAt(now)
                .build();

        cancelBooking(booking, cancelledBy, now);
        cancellation = cancellationRepository.save(cancellation);
        notifyCancellation(booking, notification);

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
        if (cancellation.getStatus() != BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
        RefundBankDetails bankDetails = new RefundBankDetails(
                trimToNull(request == null ? null : request.getBankName()),
                trimToNull(request == null ? null : request.getBankCode()),
                trimToNull(request == null ? null : request.getBankAccountName()),
                trimToNull(request == null ? null : request.getBankAccountNumber()));
        validateBankDetails(bankDetails);
        cancellation.setBankName(bankDetails.bankName());
        cancellation.setBankCode(bankDetails.bankCode());
        cancellation.setBankAccountName(bankDetails.bankAccountName());
        cancellation.setBankAccountNumber(bankDetails.bankAccountNumber());
        cancellation.setStatus(BookingCancellationStatus.REFUND_REVIEW_PENDING);


        String refundEmail = trimToNull(request == null ? null : request.getRefundEmail());
        if (refundEmail != null) {
            cancellation.setRefundEmail(refundEmail);
        }
        BookingCancellation saved = cancellationRepository.save(cancellation);
        if (refundEmail != null) {
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
                        refundExpected));
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
    private RefundBankDetails resolveRefundBankDetails(Order order, BookingCancellationRequest request) {
        if (request != null && (!isBlank(request.getBankName()) || !isBlank(request.getBankCode())
                || !isBlank(request.getBankAccountName()) || !isBlank(request.getBankAccountNumber()))) {
            return new RefundBankDetails(
                    trimToNull(request.getBankName()),
                    trimToNull(request.getBankCode()),
                    trimToNull(request.getBankAccountName()),
                    trimToNull(request.getBankAccountNumber()));
        }
        if (order == null || order.getPaymentTransactions() == null) {
            return RefundBankDetails.empty();
        }
        return order.getPaymentTransactions().stream()
                .filter(transaction -> transaction.getCashFlowType() == CashFlowType.RECEIPT)
                .filter(transaction -> !isBlank(transaction.getCounterAccountName())
                        && !isBlank(transaction.getCounterAccountNumber())
                        && (!isBlank(transaction.getCounterAccountBankName())
                        || !isBlank(transaction.getCounterAccountBankId())))
                .max(Comparator.comparing(
                        PaymentTransaction::getTransactionDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(transaction -> new RefundBankDetails(
                        trimToNull(isBlank(transaction.getCounterAccountBankName())
                                ? transaction.getCounterAccountBankId()
                                : transaction.getCounterAccountBankName()),
                        trimToNull(transaction.getCounterAccountBankId()),
                        trimToNull(transaction.getCounterAccountName()),
                        trimToNull(transaction.getCounterAccountNumber())))
                .orElseGet(RefundBankDetails::empty);
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
        return toResponse(cancellationRepository.save(cancellation));
    }

    @Transactional
    public BookingCancellationResponse confirmRefund(
            String cancellationId,
            BookingCancellationReviewRequest request,
            Account account) {
        BookingCancellation cancellation = getCancellationForUpdate(cancellationId);
        ensureBranchAccess(cancellation.getBooking(), account);
        if (cancellation.getStatus() != BookingCancellationStatus.REFUND_APPROVED) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
        if (cancellation.getRefundAmount().signum() <= 0) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }

        Order order = cancellation.getBooking().getOrder();
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        BigDecimal paidAmount = zeroIfNull(order.getPaidAmount());
        if (cancellation.getRefundAmount().compareTo(paidAmount) > 0) {
            throw new AppException(ErrorCode.CANCELLATION_REFUND_EXCEEDS_PAID);
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentTransaction refundTransaction = PaymentTransaction.builder()
                .order(order)
                .amount(cancellation.getRefundAmount())
                .paymentType(PaymentType.BANK)
                .cashFlowType(CashFlowType.REFUND)
                .note("Hoàn tiền hủy booking " + cancellation.getBooking().getId())
                .transactionDate(now)
                .build();
        paymentRepository.save(refundTransaction);

        BigDecimal remainingPaidAmount = paidAmount.subtract(cancellation.getRefundAmount());
        order.setPaidAmount(remainingPaidAmount);
        order.setPaymentStatus(remainingPaidAmount.signum() == 0
                ? PaymentStatus.UNPAID
                : PaymentStatus.PARTIAL);
        order.setOrderStatus(OrderStatusType.CANCELLED);

        cancellation.setStatus(BookingCancellationStatus.REFUNDED);
        cancellation.setRefundedBy(account);
        cancellation.setRefundedAt(now);
        cancellation.setRefundReference(request == null ? null : trimToNull(request.getNote()));
        BookingCancellation saved = cancellationRepository.save(cancellation);
        Booking booking = cancellation.getBooking();
        if (booking.getHotel() != null) {
            bookingSocketEmitter.emitRefundCompleted(
                    booking.getHotel().getId(),
                    booking.getId(),
                    cancellation.getRefundAmount());
        }
        String refundEmail = trimToNull(cancellation.getRefundEmail());
        if (refundEmail == null) {
            refundEmail = resolveRefundEmail(booking, null, cancellation.getRequestedByCustomerId(), null);
        }
        if (refundEmail != null) {
            String recipientEmail = refundEmail;
            String customerName = booking.getCustomer() == null ? null : booking.getCustomer().getFullName();
            String hotelName = hotelName(booking);
            String amount = formatMoney(cancellation.getRefundAmount());
            String refundReference = cancellation.getRefundReference();
            String bookingId = booking.getId();
            afterCommit("refund completion email", booking.getId(), () ->
                    emailService.sendRefundCompletedEmail(
                            recipientEmail,
                            customerName,
                            bookingId,
                            hotelName,
                            amount,
                            refundReference));
        } else {
            log.warn("No customer email is available for completed refund, bookingId={}", booking.getId());
        }
        return toResponse(saved);
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

        paymentService.cancelOutstandingPaymentLinks(bookingId);
        LocalDateTime now = LocalDateTime.now();
        CancellationPolicy policy = cancellationPolicy(booking, now);
        RefundBreakdown breakdown = calculateRefund(booking.getOrder(), policy.roomPenaltyRate());
        RefundBankDetails bankDetails = resolveRefundBankDetails(booking.getOrder(), null);
        cancelBooking(booking, "SYSTEM:PAYMENT_TIMEOUT", now);
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
                .refundPolicy(policy.name())
                .bankName(bankDetails.bankName())
                .bankCode(bankDetails.bankCode())
                .bankAccountName(bankDetails.bankAccountName())
                .bankAccountNumber(bankDetails.bankAccountNumber())
                .cancelledAt(now)
                .build();
        cancellationRepository.save(cancellation);
        notifyCancellation(booking, "Quá thời gian chờ thanh toán");
    }

    @Transactional
    public BookingCancellationResponse submitRefundBankDetails(
            String bookingId,
            BookingCancellationRequest request,
            Account account) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        String customerId = account.getCustomer() == null ? null : account.getCustomer().getId();
        if (customerId == null || booking.getCustomer() == null
                || !customerId.equals(booking.getCustomer().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        BookingCancellation cancellation = cancellationRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.CANCELLATION_NOT_FOUND));
        if (cancellation.getStatus() != BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_STATUS);
        }
        RefundBankDetails bankDetails = resolveRefundBankDetails(booking.getOrder(), request);
        validateBankDetails(bankDetails);
        cancellation.setBankName(bankDetails.bankName());
        cancellation.setBankCode(bankDetails.bankCode());
        cancellation.setBankAccountName(bankDetails.bankAccountName());
        cancellation.setBankAccountNumber(bankDetails.bankAccountNumber());
        cancellation.setStatus(BookingCancellationStatus.REFUND_REVIEW_PENDING);
        return toResponse(cancellationRepository.save(cancellation));
    }

    private void validateBankDetails(RefundBankDetails bankDetails) {
        if (bankDetails == null || !bankDetails.isComplete()) {
            throw new AppException(ErrorCode.CANCELLATION_BANK_DETAILS_REQUIRED);
        }
        String accountNumber = bankDetails.bankAccountNumber();
        if (!accountNumber.matches("[0-9]{6,50}")) {
            throw new AppException(ErrorCode.CANCELLATION_INVALID_BANK_ACCOUNT);
        }
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
        Duration timeUntilCheckIn = Duration.between(now, checkIn);
        long secondsBeforeCheckIn = timeUntilCheckIn.getSeconds();
        long hoursBeforeCheckIn = Math.max(0, timeUntilCheckIn.toHours());
        if (secondsBeforeCheckIn >= Duration.ofHours(72).getSeconds()) {
            return new CancellationPolicy(BigDecimal.ZERO, "CANCEL_AT_LEAST_72_HOURS_BEFORE_CHECK_IN", hoursBeforeCheckIn);
        }
        if (secondsBeforeCheckIn >= Duration.ofHours(24).getSeconds()) {
            return new CancellationPolicy(new BigDecimal("0.50"), "CANCEL_24_TO_72_HOURS_BEFORE_CHECK_IN", hoursBeforeCheckIn);
        }
        return new CancellationPolicy(BigDecimal.ONE, "CANCEL_WITHIN_24_HOURS_OR_NO_SHOW", hoursBeforeCheckIn);
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

    private RefundBreakdown calculateRefund(Order order, BigDecimal roomPenaltyRate) {
        if (order == null) {
            return RefundBreakdown.zero();
        }
        BigDecimal roomGross = zeroIfNull(order.getRoomTotalAmount());
        BigDecimal serviceGross = zeroIfNull(order.getServiceTotalAmount());
        BigDecimal surchargeGross = zeroIfNull(order.getSurchargeTotalAmount());
        BigDecimal coreGross = roomGross.add(serviceGross);

        BigDecimal roomDiscount = zeroIfNull(order.getDiscountRoomAmount());
        BigDecimal serviceDiscount = zeroIfNull(order.getDiscountServiceAmount());
        BigDecimal totalDiscount = zeroIfNull(order.getDiscountAmountTotal());
        BigDecimal totalRoomDiscountShare = coreGross.signum() == 0
                ? BigDecimal.ZERO
                : totalDiscount.multiply(roomGross).divide(coreGross, 2, RoundingMode.HALF_UP);
        BigDecimal totalServiceDiscountShare = totalDiscount.subtract(totalRoomDiscountShare);

        BigDecimal roomNet = roomGross.subtract(roomDiscount).subtract(totalRoomDiscountShare).max(BigDecimal.ZERO);
        BigDecimal serviceNet = serviceGross.subtract(serviceDiscount).subtract(totalServiceDiscountShare).max(BigDecimal.ZERO);
        BigDecimal payable = roomNet.add(serviceNet).add(surchargeGross);
        BigDecimal paid = zeroIfNull(order.getPaidAmount()).min(payable);
        if (payable.signum() <= 0 || paid.signum() <= 0) {
            return RefundBreakdown.zero();
        }

        BigDecimal paidRoom = paid.multiply(roomNet).divide(payable, 2, RoundingMode.HALF_UP);
        BigDecimal paidService = paid.multiply(serviceNet).divide(payable, 2, RoundingMode.HALF_UP);
        BigDecimal roomPenalty = paidRoom.multiply(roomPenaltyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal roomRefund = paidRoom.subtract(roomPenalty).max(BigDecimal.ZERO);
        BigDecimal serviceRefund = paidService;
        BigDecimal refund = roomRefund.add(serviceRefund).min(paid).setScale(2, RoundingMode.HALF_UP);
        return new RefundBreakdown(
                refund,
                roomRefund.setScale(2, RoundingMode.HALF_UP),
                serviceRefund.setScale(2, RoundingMode.HALF_UP),
                roomPenalty.setScale(2, RoundingMode.HALF_UP));
    }

    private void cancelBooking(Booking booking, String cancelledBy, LocalDateTime cancelledAt) {
        booking.setBookingStatus(BookingStatus.CANCELLED);
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
                .customerId(booking.getCustomer() == null ? null : booking.getCustomer().getId())
                .customerName(booking.getCustomer() == null ? null : booking.getCustomer().getFullName())
                .customerPhone(booking.getCustomer() == null ? null : booking.getCustomer().getPhone())
                .status(cancellation.getStatus())
                .reason(cancellation.getReason())
                .refundAmount(cancellation.getRefundAmount())
                .roomRefundAmount(cancellation.getRoomRefundAmount())
                .serviceRefundAmount(cancellation.getServiceRefundAmount())
                .roomPenaltyAmount(cancellation.getRoomPenaltyAmount())
                .hoursBeforeCheckIn(cancellation.getHoursBeforeCheckIn())
                .refundPolicy(cancellation.getRefundPolicy())
                .bankName(cancellation.getBankName())
                .bankCode(cancellation.getBankCode())
                .bankAccountName(cancellation.getBankAccountName())
                .bankAccountNumber(cancellation.getBankAccountNumber())
                .reviewNote(cancellation.getReviewNote())
                .refundReference(cancellation.getRefundReference())
                .requestedAt(cancellation.getRequestedAt())
                .reviewedAt(cancellation.getReviewedAt())
                .refundedAt(cancellation.getRefundedAt())
                .build();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
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

    private record RefundBreakdown(
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
                    && bankAccountName != null && !bankAccountName.isBlank()
                    && bankAccountNumber != null && !bankAccountNumber.isBlank();
        }
    }
}
