package iuh.fit.se.hotelmanagement_be.modular.booking.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import org.springframework.stereotype.Component;

@Component
public class RoomPricingCalculator {
    /**
     * Tính tiền phụ thu mỗi đêm dựa trên BranchRoomPolicy và số lượng khách thực tế.
     * Áp dụng thuật toán Capacity Weight System (Adult = 1.0, Child = 0.5) và quy chế bù chênh lệch.
     */
    public double calculateExtraFeeWithCapacityWeight(
            BranchRoomPolicy policy,
            int actualAdults,
            int actualChildren,
            int actualInfants) {

        // --- BƯỚC 1: TRỌNG SỐ QUY ĐỔI & GIỚI HẠN ---
        double adultWeight = 1.0;
        double childWeight = 0.5;

        double actualWeight = (actualAdults * adultWeight) + (actualChildren * childWeight);
        double standardWeight = (policy.getStandardAdults() * adultWeight) + (policy.getMaxChildren() * childWeight);
        double maxWeightLimit = (policy.getMaxAdults() * adultWeight) + (policy.getMaxChildren() * childWeight);

        // --- BƯỚC 2: VALIDATE AN TOÀN TRẦN VẬT LÝ ---
        if (actualInfants > policy.getMaxInfants()) {
            throw new AppException(ErrorCode.EXCEEDS_MAX_INFANTS);
        }
        if (actualWeight > maxWeightLimit) {
            throw new AppException(ErrorCode.EXCEEDS_MAX_CAPACITY);
        }

        // --- BƯỚC 3: NẾU ĐI BẰNG HOẶC ÍT HƠN TIÊU CHUẨN -> MIỄN PHÍ PHỤ THU ---
        if (actualWeight <= standardWeight) {
            return 0.0;
        }

        // --- BƯỚC 4: TÍNH PHỤ THU & BÙ CHÊNH LỆCH ---
        int extraAdults = Math.max(0, actualAdults - policy.getStandardAdults());
        int unusedChildSlots = Math.max(0, policy.getMaxChildren() - actualChildren);

        // Cứ 2 suất Trẻ em không dùng -> Đổi lấy 1 Người lớn MIỄN PHÍ
        int freeAdultsFromTwoChildren = unusedChildSlots / 2;
        int remainingUnusedChildSlots = unusedChildSlots % 2;

        int payableAdults = Math.max(0, extraAdults - freeAdultsFromTwoChildren);

        double extraFee = 0.0;

        // Nếu còn dư đúng 1 suất Trẻ em mà có 1 Người lớn cần đổi -> BÙ CHÊNH LỆCH (ExtraAdultFee - ExtraChildFee)
        if (payableAdults > 0 && remainingUnusedChildSlots == 1) {
            double compensationFee = policy.getExtraAdultFee() - policy.getExtraChildFee();
            extraFee = compensationFee + ((payableAdults - 1) * policy.getExtraAdultFee());
        } else {
            extraFee = payableAdults * policy.getExtraAdultFee();
        }

        // Cộng thêm số trẻ em đi vượt định mức (nếu có)
        int extraChildren = Math.max(0, actualChildren - policy.getMaxChildren());
        extraFee += extraChildren * policy.getExtraChildFee();

        return extraFee;
    }
}
