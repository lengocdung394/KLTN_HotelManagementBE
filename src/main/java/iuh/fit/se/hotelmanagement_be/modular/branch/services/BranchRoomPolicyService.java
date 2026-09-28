package iuh.fit.se.hotelmanagement_be.modular.branch.services;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.BranchRoomPolicy;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.BranchRoomPolicyRequest;

import java.util.List;

public interface BranchRoomPolicyService {
    // Lay ds gia theo chinh sach cua tung loai phong
    List<BranchRoomPolicy> getPoliciesByHotel(Long hotelId);

    // update  chinh sach cua tung loai phong
    BranchRoomPolicy updateRoomPolicy(String policyId, BranchRoomPolicyRequest request, Account currentAccount);

}
