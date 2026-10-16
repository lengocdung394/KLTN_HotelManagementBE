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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

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
    @Transactional
    public BranchRoomPolicy updateRoomPolicy(
            String id,
            BranchRoomPolicyRequest request,
            Account currentAccount,
            boolean isSuperAdmin) {

        if (!isSuperAdmin
                && (currentAccount == null || currentAccount.getHotelId() == null)) {
            throw new AppException(ErrorCode.MANAGER_HOTEL_NOT_ASSIGNED);
        }

        BranchRoomPolicy policy = isSuperAdmin
                ? branchRoomPolicyRepository.findById(id)
                .orElseThrow(() ->
                        new AppException(ErrorCode.BRANCH_POLICY_NOT_FOUND))
                : branchRoomPolicyRepository
                .findByIdAndHotelId(id, currentAccount.getHotelId())
                .orElseThrow(() ->
                        new AppException(ErrorCode.BRANCH_POLICY_NOT_FOUND));

        // Lưu trạng thái cũ trước khi thay đổi policy
        Double oldBasePrice = policy.getBasePrice();
        Double oldExtraAdultFee = policy.getExtraAdultFee();
        Double oldExtraChildFee = policy.getExtraChildFee();
        Double oldArea = policy.getArea();
        Integer oldStandardCapacity = policy.getStandardCapacity();
        Integer oldMaxExtraGuests = policy.getMaxExtraGuests();

// Chỉ cập nhật các trường được gửi lên
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

        BranchRoomPolicy saved = branchRoomPolicyRepository.save(policy);
        Long hotelId = saved.getHotel().getId();

        List<Map<String, Object>> changes = new ArrayList<>();
        addChange(changes, "Giá cơ bản", oldBasePrice, saved.getBasePrice());
        addChange(changes, "Phụ thu người lớn", oldExtraAdultFee, saved.getExtraAdultFee());
        addChange(changes, "Phụ thu trẻ em", oldExtraChildFee, saved.getExtraChildFee());
        addChange(changes, "Diện tích", oldArea, saved.getArea());
        addChange(changes, "Sức chứa tiêu chuẩn", oldStandardCapacity, saved.getStandardCapacity());
        addChange(changes, "Số khách phụ tối đa", oldMaxExtraGuests, saved.getMaxExtraGuests());

        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("id", saved.getId());
        eventData.put("roomType", saved.getRoomType());
        eventData.put("changes", changes);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    branchSocketEmitter.emitRoomPolicyUpdate(hotelId, eventData, isSuperAdmin);
                }
            });
        } else {
            branchSocketEmitter.emitRoomPolicyUpdate(hotelId, eventData, isSuperAdmin);
        }
        return saved;
    }
    private void addChange(
            List<Map<String, Object>> changes,
            String label,
            Object before,
            Object after
    ) {
        if (Objects.equals(before, after)) {
            return;
        }

        Map<String, Object> change = new LinkedHashMap<>();
        change.put("label", label);
        change.put("before", before);
        change.put("after", after);
        changes.add(change);
    }

}
