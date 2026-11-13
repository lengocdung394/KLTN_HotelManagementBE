package iuh.fit.se.hotelmanagement_be.modular.branch.services;

import iuh.fit.se.hotelmanagement_be.modular.branch.requests.BranchRoomPolicyRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.requests.SuperAdminCreateBranchRequest;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.SuperAdminBranchDetailResponse;
import iuh.fit.se.hotelmanagement_be.modular.branch.responses.SuperAdminBranchSummaryResponse;

import java.util.List;

public interface SuperAdminService {
//    List<SuperAdminBranchDetailResponse.RoomPolicyItem> saveBranchRoomPolicies(
//            Long hotelId,
//            List<BranchRoomPolicyRequest> requests);
    List<SuperAdminBranchSummaryResponse> getBranches();

    List<SuperAdminBranchSummaryResponse> getBranchesByProvince(String provinceName);

    SuperAdminBranchSummaryResponse createBranch(SuperAdminCreateBranchRequest request);

    SuperAdminBranchDetailResponse getBranchDetails(Long hotelId);
}
