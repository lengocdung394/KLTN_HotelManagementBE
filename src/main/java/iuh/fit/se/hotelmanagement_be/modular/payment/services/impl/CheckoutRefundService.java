package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.CheckoutRefund;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.CheckoutRefundStatus;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.Order;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.PaymentTransaction;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.CashFlowType;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentStatus;
import iuh.fit.se.hotelmanagement_be.modular.payment.entities.enums.PaymentType;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.CheckoutRefundRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.repositories.PaymentRepository;
import iuh.fit.se.hotelmanagement_be.modular.payment.requests.CheckoutRefundReviewRequest;
import iuh.fit.se.hotelmanagement_be.modular.payment.responses.CheckoutRefundResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class CheckoutRefundService {
    CheckoutRefundRepository checkoutRefundRepository;
    PaymentRepository paymentRepository;

    public List<CheckoutRefundResponse> getRefunds(Account account) {
        List<CheckoutRefund> refunds;
        if (isSuperAdmin(account)) {
            refunds = checkoutRefundRepository.findAllByOrderByRequestedAtDesc();
        } else {
            Long hotelId = account.getHotelId();
            if (hotelId == null) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            refunds = checkoutRefundRepository.findAllByHotelIdOrderByRequestedAtDesc(hotelId);
        }
        return refunds.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CheckoutRefundResponse updateBankDetails(
            String refundId,
            CheckoutRefundReviewRequest request,
            Account account) {
        CheckoutRefund refund = getRefundForAccount(refundId, account);
        if (refund.getStatus() != CheckoutRefundStatus.PENDING) {
            throw new AppException(ErrorCode.CHECKOUT_REFUND_INVALID_STATUS);
        }
        setBankDetails(refund, request);
        return toResponse(checkoutRefundRepository.save(refund));
    }

    @Transactional
    public CheckoutRefundResponse approve(String refundId, CheckoutRefundReviewRequest request, Account account) {
        CheckoutRefund refund = getRefundForAccount(refundId, account);
        if (refund.getStatus() != CheckoutRefundStatus.PENDING) {
            throw new AppException(ErrorCode.CHECKOUT_REFUND_INVALID_STATUS);
        }
        if (request == null || !Boolean.TRUE.equals(request.getRefundAmountVerified())
                || !Boolean.TRUE.equals(request.getRefundAccountConfirmed())) {
            throw new AppException(ErrorCode.CANCELLATION_REVIEW_CONFIRMATION_REQUIRED);
        }
        if (hasBankDetails(request)) {
            setBankDetails(refund, request);
        }
        validateBankDetails(refund);
        refund.setStatus(CheckoutRefundStatus.APPROVED);
        refund.setApprovedBy(account);
        refund.setApprovedAt(LocalDateTime.now());
        if (request != null && request.getNote() != null && !request.getNote().isBlank()) {
            refund.setNote(request.getNote().trim());
        }
        return toResponse(checkoutRefundRepository.save(refund));
    }

    @Transactional
    public CheckoutRefundResponse confirmRefund(
            String refundId,
            CheckoutRefundReviewRequest request,
            Account account) {
        CheckoutRefund refund = getRefundForAccount(refundId, account);
        boolean cashRefundPending = refund.getRefundMethod() == PaymentType.CASH
                && refund.getStatus() == CheckoutRefundStatus.PENDING;
        if (!cashRefundPending && refund.getStatus() != CheckoutRefundStatus.APPROVED) {
            throw new AppException(ErrorCode.CHECKOUT_REFUND_INVALID_STATUS);
        }
        Order order = refund.getBooking().getOrder();
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        BigDecimal paidAmount = zeroIfNull(order.getPaidAmount());
        if (refund.getAmount().compareTo(paidAmount) > 0) {
            throw new AppException(ErrorCode.CANCELLATION_REFUND_EXCEEDS_PAID);
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .amount(refund.getAmount())
                .paymentType(refund.getRefundMethod())
                .cashFlowType(CashFlowType.REFUND)
                .note((refund.getRefundMethod() == PaymentType.CASH
                        ? "Hoàn dư tiền mặt tại quầy khi check-out booking "
                        : "Hoàn dư qua ngân hàng khi check-out booking ")
                        + refund.getBooking().getId()
                        + (request == null || request.getNote() == null || request.getNote().isBlank()
                        ? "" : " · Ghi chú: " + request.getNote().trim()))
                .transactionDate(now)
                .build();
        paymentRepository.save(transaction);

        BigDecimal remainingPaid = paidAmount.subtract(refund.getAmount());
        order.setPaidAmount(remainingPaid);
        order.setRemainingAmount(zeroIfNull(order.getTotalAmount()).subtract(remainingPaid).max(BigDecimal.ZERO));
        order.setPaymentStatus(remainingPaid.signum() == 0
                ? PaymentStatus.UNPAID
                : remainingPaid.compareTo(zeroIfNull(order.getTotalAmount())) >= 0
                ? PaymentStatus.PAID
                : PaymentStatus.PARTIAL);

        refund.setStatus(CheckoutRefundStatus.REFUNDED);
        refund.setRefundedBy(account);
        refund.setRefundedAt(now);
        if (request != null && request.getNote() != null && !request.getNote().isBlank()) {
            refund.setNote(request.getNote().trim());
        }
        return toResponse(checkoutRefundRepository.save(refund));
    }

    private CheckoutRefund getRefundForAccount(String refundId, Account account) {
        CheckoutRefund refund = checkoutRefundRepository.findById(refundId)
                .orElseThrow(() -> new AppException(ErrorCode.CANCELLATION_NOT_FOUND));
        ensureHotelAccess(refund.getBooking(), account);
        return refund;
    }

    private void ensureHotelAccess(Booking booking, Account account) {
        if (isSuperAdmin(account)) {
            return;
        }
        Long hotelId = account.getHotelId();
        if (hotelId == null || booking.getHotel() == null || !hotelId.equals(booking.getHotel().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private boolean isSuperAdmin(Account account) {
        return account.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    private boolean hasBankDetails(CheckoutRefundReviewRequest request) {
        return request != null && (notBlank(request.getBankName())
                || notBlank(request.getBankCode())
                || notBlank(request.getBankAccountName())
                || notBlank(request.getBankAccountNumber()));
    }

    private void setBankDetails(CheckoutRefund refund, CheckoutRefundReviewRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.CANCELLATION_BANK_DETAILS_REQUIRED);
        }
        refund.setBankName(trimToNull(request.getBankName()));
        refund.setBankCode(trimToNull(request.getBankCode()));
        refund.setBankAccountName(trimToNull(request.getBankAccountName()));
        refund.setBankAccountNumber(trimToNull(request.getBankAccountNumber()));
        validateBankDetails(refund);
    }

    private void validateBankDetails(CheckoutRefund refund) {
        if (!notBlank(refund.getBankName()) || !notBlank(refund.getBankAccountName())
                || refund.getBankAccountNumber() == null
                || !refund.getBankAccountNumber().matches("[0-9]{6,50}")) {
            throw new AppException(ErrorCode.CANCELLATION_BANK_DETAILS_REQUIRED);
        }
    }

    private CheckoutRefundResponse toResponse(CheckoutRefund refund) {
        Booking booking = refund.getBooking();
        Order order = booking.getOrder();
        return CheckoutRefundResponse.builder()
                .id(refund.getId())
                .bookingId(booking.getId())
                .customerName(booking.getCustomer() == null ? null : booking.getCustomer().getFullName())
                .customerPhone(booking.getCustomer() == null ? null : booking.getCustomer().getPhone())
                .hotelId(booking.getHotel() == null ? null : booking.getHotel().getId())
                .paidAmount(order == null
                        ? BigDecimal.ZERO
                        : zeroIfNull(order.getPaidAmount()).add(
                                refund.getStatus() == CheckoutRefundStatus.REFUNDED
                                        ? zeroIfNull(refund.getAmount())
                                        : BigDecimal.ZERO))
                .finalAmount(order == null ? BigDecimal.ZERO : zeroIfNull(order.getTotalAmount()))
                .refundAmount(refund.getAmount())
                .refundMethod(refund.getRefundMethod())
                .status(refund.getStatus())
                .bankName(refund.getBankName())
                .bankCode(refund.getBankCode())
                .bankAccountName(refund.getBankAccountName())
                .bankAccountNumber(refund.getBankAccountNumber())
                .note(refund.getNote())
                .requestedAt(refund.getRequestedAt())
                .approvedAt(refund.getApprovedAt())
                .refundedAt(refund.getRefundedAt())
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

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
