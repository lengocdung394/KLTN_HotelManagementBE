package iuh.fit.se.hotelmanagement_be.modular.payment.services.impl;

import iuh.fit.se.hotelmanagement_be.modular.booking.entities.BookingCancellation;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingCancellationStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.enums.BookingStatus;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingCancellationRepository;
import iuh.fit.se.hotelmanagement_be.modular.booking.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RefundBankDetailsMigration implements ApplicationRunner {
    private final BookingCancellationRepository cancellationRepository;
    private final BookingRepository bookingRepository;
    private final RefundBankDetailsCrypto crypto;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<BookingCancellation> cancellations = cancellationRepository.findAll();
        boolean changed = false;
        for (BookingCancellation cancellation : cancellations) {
            changed |= migrateLegacyBookingStatus(cancellation);
            if (cancellation.getStatus() == BookingCancellationStatus.REFUNDED
                    || cancellation.getStatus() == BookingCancellationStatus.NO_REFUND_REQUIRED
                    || cancellation.getStatus() == BookingCancellationStatus.REFUND_REJECTED) {
                changed |= clearBankDetails(cancellation);
                continue;
            }

            changed |= encryptLegacyValue(cancellation.getBankName(), cancellation::setBankName);
            changed |= encryptLegacyValue(cancellation.getBankCode(), cancellation::setBankCode);
            changed |= encryptLegacyValue(cancellation.getBankAccountName(), cancellation::setBankAccountName);
            changed |= encryptLegacyValue(cancellation.getBankAccountNumber(), cancellation::setBankAccountNumber);
            if (hasBankDetails(cancellation) && cancellation.getBankDetailsUpdatedAt() == null) {
                cancellation.setBankDetailsUpdatedAt(
                        cancellation.getRequestedAt() == null ? LocalDateTime.now() : cancellation.getRequestedAt());
                changed = true;
            }
        }
        if (changed) {
            cancellationRepository.saveAll(cancellations);
        }
    }

    private boolean migrateLegacyBookingStatus(BookingCancellation cancellation) {
        Booking booking = cancellation.getBooking();
        if (booking.getBookingStatus() != BookingStatus.CANCELLED) {
            return false;
        }
        String reason = cancellation.getReason() == null ? "" : cancellation.getReason().toLowerCase();
        BookingStatus migratedStatus;
        if (reason.contains("quá thời gian") || reason.contains("timeout")) {
            migratedStatus = BookingStatus.CANCELLED_PAYMENT_TIMEOUT;
        } else if (cancellation.getRequestedByCustomerId() != null) {
            migratedStatus = BookingStatus.CANCELLED_BY_CUSTOMER;
        } else if (cancellation.getRequestedByAccount() != null) {
            migratedStatus = BookingStatus.CANCELLED_BY_STAFF;
        } else {
            return false;
        }
        booking.setBookingStatus(migratedStatus);
        bookingRepository.save(booking);
        return true;
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void expireStaleRefundBankDetails() {
        LocalDateTime expiration = LocalDateTime.now().minusDays(30);
        List<BookingCancellation> expired = cancellationRepository.findExpiredBankDetails(
                List.of(
                        BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED,
                        BookingCancellationStatus.REFUND_REVIEW_PENDING,
                        BookingCancellationStatus.REFUND_APPROVED,
                        BookingCancellationStatus.PAYOUT_FAILED),
                expiration);
        for (BookingCancellation cancellation : expired) {
            if (cancellation.getRefundAmount() == null || cancellation.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
                clearBankDetails(cancellation);
                cancellation.setStatus(BookingCancellationStatus.NO_REFUND_REQUIRED);
                continue;
            }
            clearBankDetails(cancellation);
            cancellation.setStatus(BookingCancellationStatus.REFUND_BANK_DETAILS_REQUIRED);
            cancellation.setReviewedBy(null);
            cancellation.setReviewedAt(null);
            if ("FAILED".equals(cancellation.getPayoutState())) {
                cancellation.setPayoutId(null);
                cancellation.setPayoutReference(null);
                cancellation.setPayoutState(null);
                cancellation.setPayoutError(null);
            }
        }
        if (!expired.isEmpty()) {
            cancellationRepository.saveAll(expired);
        }
    }

    private boolean encryptLegacyValue(String value, java.util.function.Consumer<String> setter) {
        if (value == null || value.isBlank() || crypto.isEncrypted(value)) {
            return false;
        }
        setter.accept(crypto.encrypt(value));
        return true;
    }

    private boolean clearBankDetails(BookingCancellation cancellation) {
        boolean changed = hasBankDetails(cancellation);
        cancellation.setBankName(null);
        cancellation.setBankCode(null);
        cancellation.setBankAccountName(null);
        cancellation.setBankAccountNumber(null);
        cancellation.setBankDetailsUpdatedAt(null);
        return changed;
    }

    private boolean hasBankDetails(BookingCancellation cancellation) {
        return cancellation.getBankName() != null
                || cancellation.getBankCode() != null
                || cancellation.getBankAccountName() != null
                || cancellation.getBankAccountNumber() != null;
    }
}
