package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.booking.responses.ExtraFeeBreakdownResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import org.springframework.stereotype.Component;

@Component
public class RoomPricingCalculator {
    /**
     * Tính tiền phụ thu chi tiết dựa trên sức chứa tiêu chuẩn và sức chứa phụ thu tối đa.
     */
    public ExtraFeeBreakdownResponse calculateExtraFeeBreakdown(
            BranchRoomPolicy policy,
            int actualAdults,
            int actualChildren) {

        // --- BƯỚC 1: VALIDATE TỔNG SỨC CHỨA TỐI ĐA ---
        int totalActualGuests = actualAdults + actualChildren;
        int maxCapacity = policy.getMaxCapacity();

        if (totalActualGuests > maxCapacity) {
            throw new AppException(ErrorCode.EXCEEDS_MAX_CAPACITY);
        }

        // --- BƯỚC 2: NẾU TỔNG KHÁCH <= TIÊU CHUẨN -> MIỄN PHÍ HOÀN TOÀN ---
        int standardCapacity = policy.getStandardCapacity();
        if (totalActualGuests <= standardCapacity) {
            return new ExtraFeeBreakdownResponse(0.0, 0.0, 0.0);
        }

        // --- BƯỚC 3: TÍNH SỐ LƯỢNG KHÁCH PHÁT SINH ---
        int extraGuests = totalActualGuests - standardCapacity;
        if (extraGuests > policy.getMaxExtraGuests()) {
            throw new AppException(ErrorCode.EXCEEDS_MAX_EXTRA_GUESTS);
        }

        // --- BƯỚC 4: PHÂN BỔ SUẤT TIÊU CHUẨN & TÍNH TIỀN TỪNG LOẠI ---
        // Ưu tiên lấp đầy suất tiêu chuẩn cho Người lớn trước, phần còn dư suất tiêu chuẩn cho Trẻ em hưởng ké
        int actualAdultsCoveredByStandard = Math.min(actualAdults, standardCapacity);
        int remainingStandardSlots = standardCapacity - actualAdultsCoveredByStandard;

        int extraAdults = Math.max(0, actualAdults - actualAdultsCoveredByStandard);
        int extraChildren = Math.max(0, actualChildren - remainingStandardSlots);

        // Tính tiền
        double adultFee = extraAdults * policy.getExtraAdultFee();
        double childFee = extraChildren * policy.getExtraChildFee();
        double totalFee = adultFee + childFee;

        return new ExtraFeeBreakdownResponse(adultFee, childFee, totalFee);
    }
}
