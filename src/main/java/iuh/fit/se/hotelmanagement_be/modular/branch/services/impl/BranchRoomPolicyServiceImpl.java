package iuh.fit.se.hotelmanagement_be.modular.branch.services.impl;

import iuh.fit.se.hotelmanagement_be.exception.AppException;
import iuh.fit.se.hotelmanagement_be.exception.ErrorCode;
import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.BranchRoomPolicyRepository;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.BranchRoomPolicyRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.services.BranchRoomPolicyService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BranchRoomPolicyServiceImpl implements BranchRoomPolicyService {
    BranchRoomPolicyRepository branchRoomPolicyRepository;
    BranchSocketEmitter branchSocketEmitter;

    // chinh sua gia cho tung loai phong o chi nhanh
    @Override
    public List<BranchRoomPolicy> getPoliciesByHotel(Long hotelId) {
        return branchRoomPolicyRepository.findByHotelId(hotelId);
    }
    // ham lay ra ds loai phong o mot chi nhanh

    @Override
    public BranchRoomPolicy updateRoomPolicy(String id, BranchRoomPolicyRequest request, Account currentAccount) {
        // 1. Tìm chính sách phòng theo ID
        Long hotelId = currentAccount != null ? currentAccount.getHotelId() : null;
        BranchRoomPolicy policy = (hotelId != null
                ? branchRoomPolicyRepository.findByIdAndHotelId(id, hotelId)
                : branchRoomPolicyRepository.findById(id))
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_POLICY_NOT_FOUND));

        // 2. Cập nhật các thông tin từ request (ví dụ: giá cơ bản, phụ thu, diện tích, sức chứa...)
        if (request.getBasePrice() != null) {
            policy.setBasePrice(request.getBasePrice());
        }
        if (request.getExtraAdultFee() != null) {
            policy.setExtraAdultFee(request.getExtraAdultFee());
        }
        if (request.getExtraChildFee() != null) {
            policy.setExtraChildFee(request.getExtraChildFee());
        }
        if (request.getArea() != null) {
            policy.setArea(request.getArea());
        }
        if (request.getStandardCapacity() != null) {
            policy.setStandardCapacity(request.getStandardCapacity());
        }
        if (request.getMaxExtraGuests() != null) {
            policy.setMaxExtraGuests(request.getMaxExtraGuests());
        }
        // 3. Lưu xuống Database (chỉ gọi save 1 lần duy nhất)
        BranchRoomPolicy updatedPolicy = branchRoomPolicyRepository.save(policy);

        log.info("cap nhat thanh cong{}", updatedPolicy);

        // 4. BẮN SOCKET THÔNG BÁO REAL-TIME NGAY LẬP TỨC
        if (policy.getHotel() != null) {
            branchSocketEmitter.emitRoomPolicyUpdate(policy.getHotel().getId(), updatedPolicy);
        }

        // 5. Trả về kết quả sau khi đã cập nhật
        return updatedPolicy;
    }


}
