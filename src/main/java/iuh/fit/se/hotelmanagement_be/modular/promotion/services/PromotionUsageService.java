package iuh.fit.se.hotelmanagement_be.modular.promotion.services;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Customer;
import iuh.fit.se.hotelmanagement_be.modular.booking.entities.Booking;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.CustomerPromotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.Promotion;
import iuh.fit.se.hotelmanagement_be.modular.promotion.entities.UserPromotionUsage;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.CustomerPromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.PromotionRepository;
import iuh.fit.se.hotelmanagement_be.modular.promotion.repositories.UserPromotionUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromotionUsageService {
    private final PromotionRepository promotionRepository;
    private final CustomerPromotionRepository customerPromotionRepository;
    private final UserPromotionUsageRepository userPromotionUsageRepository;


    // thay doi khuyen mai trong booking
    @Transactional
    public void replaceBookingPromotion(
            Booking booking,
            Promotion nextPromotion,
            CustomerPromotion nextCustomerPromotion
    ) {
        Promotion currentPromotion = getBookingPromotion(booking);
        CustomerPromotion currentCustomerPromotion = booking.getCustomerPromotion();

        boolean samePromotion = currentPromotion != null
                && nextPromotion != null
                && Objects.equals(currentPromotion.getId(), nextPromotion.getId());

        boolean sameCustomerVoucher = currentCustomerPromotion != null
                && nextCustomerPromotion != null
                && Objects.equals(currentCustomerPromotion.getId(), nextCustomerPromotion.getId());

        if (currentPromotion != null && !samePromotion) {
            releaseUsage(booking, currentPromotion);
        }
        if (currentCustomerPromotion != null && !sameCustomerVoucher) {
            releaseCustomerVoucher(booking, currentCustomerPromotion);
        }

        booking.setPromotion(nextCustomerPromotion == null ? nextPromotion : null);
        booking.setCustomerPromotion(nextCustomerPromotion);

        if (nextPromotion != null) {
            reserveUsage(booking, nextPromotion, nextCustomerPromotion);
        }
    }

    // ham chinh cho viec thay doi ma khuyen mai
    private void reserveUsage(
            Booking booking,
            Promotion promotion,
            CustomerPromotion customerPromotion
    ) {
        Customer customer = booking.getCustomer();
        if (customer == null) {
            throw new AppException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        Promotion lockedPromotion = promotionRepository.findByIdForUpdate(promotion.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));

        Optional<UserPromotionUsage> existingUsage =
                userPromotionUsageRepository.findByCustomerAndPromotionCodeForUpdate(
                        customer.getId(),
                        lockedPromotion.getCode()
                );
        if (existingUsage.isPresent()) {
            if (!Objects.equals(existingUsage.get().getBooking().getId(), booking.getId())) {
                throw new AppException(ErrorCode.PROMOTION_ALREADY_USED_BY_USER);
            }
            markCustomerVoucherReserved(booking, customerPromotion);
            return;
        }

        int usedCount = lockedPromotion.getUsedCount() == null ? 0 : lockedPromotion.getUsedCount();
        if (lockedPromotion.getUsageLimit() != null
                && usedCount >= lockedPromotion.getUsageLimit()) {
            throw new AppException(ErrorCode.PROMOTION_OUT_OF_STOCK);
        }

        LocalDateTime appliedAt = LocalDateTime.now();

        lockedPromotion.setUsedCount(usedCount + 1); // luot khuyen mai duoc dung cung tang len mot

        promotionRepository.save(lockedPromotion);

        userPromotionUsageRepository.save(UserPromotionUsage.builder()
                .customer(customer)
                .promotionCode(lockedPromotion.getCode())
                .booking(booking)
                .usedAt(appliedAt) // thoi gian chinh thuc cua apply ma
                .build());

        markCustomerVoucherReserved(booking, customerPromotion, appliedAt);
    }

    // ham gỡ bỏ khuyến mãi trong khi chỉnh sửa booking
    private void releaseUsage(Booking booking, Promotion promotion) {
        Customer customer = booking.getCustomer();
        if (customer == null) {
            return;
        }

        Promotion lockedPromotion = promotionRepository.findByIdForUpdate(promotion.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
        Optional<UserPromotionUsage> usage =
                userPromotionUsageRepository.findByCustomerAndBookingForUpdate(
                        customer.getId(),
                        booking.getId()
                );
        if (usage.isEmpty()) {
            return;
        }

        userPromotionUsageRepository.delete(usage.get());
        int usedCount = lockedPromotion.getUsedCount() == null ? 0 : lockedPromotion.getUsedCount();
        lockedPromotion.setUsedCount(Math.max(0, usedCount - 1));
        promotionRepository.save(lockedPromotion);
    }

    private void releaseCustomerVoucher(Booking booking, CustomerPromotion customerPromotion) {
        if (booking.getCustomer() == null) {
            return;
        }

        CustomerPromotion lockedVoucher = customerPromotionRepository
                .findByIdAndCustomerIdForUpdate(customerPromotion.getId(), booking.getCustomer().getId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_PROMOTION_NOT_FOUND));
        if (lockedVoucher.getBooking() == null
                || !Objects.equals(lockedVoucher.getBooking().getId(), booking.getId())) {
            return;
        }

        lockedVoucher.setUsed(false);
        lockedVoucher.setUsedAt(null);
        lockedVoucher.setBooking(null);
        customerPromotionRepository.save(lockedVoucher);
    }

    private void markCustomerVoucherReserved(Booking booking, CustomerPromotion customerPromotion) {
        markCustomerVoucherReserved(booking, customerPromotion, LocalDateTime.now());
    }

    private void markCustomerVoucherReserved(
            Booking booking,
            CustomerPromotion customerPromotion,
            LocalDateTime appliedAt
    ) {
        if (customerPromotion == null) {
            return;
        }

        Customer customer = booking.getCustomer();
        CustomerPromotion lockedVoucher = customerPromotionRepository
                .findByIdAndCustomerIdForUpdate(customerPromotion.getId(), customer.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_PROMOTION_NOT_FOUND));
        if (lockedVoucher.isUsed()
                && (lockedVoucher.getBooking() == null
                || !Objects.equals(lockedVoucher.getBooking().getId(), booking.getId()))) {
            throw new AppException(ErrorCode.PROMOTION_ALREADY_USED);
        }
        if (lockedVoucher.getBooking() != null
                && !Objects.equals(lockedVoucher.getBooking().getId(), booking.getId())) {
            throw new AppException(ErrorCode.PROMOTION_ALREADY_USED);
        }

        lockedVoucher.setBooking(booking);
        lockedVoucher.setUsed(true);
        if (lockedVoucher.getUsedAt() == null) {
            lockedVoucher.setUsedAt(appliedAt);
        }
        customerPromotionRepository.save(lockedVoucher);
    }

    private Promotion getBookingPromotion(Booking booking) {
        return booking.getCustomerPromotion() != null
                ? booking.getCustomerPromotion().getPromotion()
                : booking.getPromotion();
    }
}
