package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.repositories.AccountRepository;
import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmailService;
import iuh.fit.se.hotelmanagement_be.modular.branch.entities.Hotel;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/contact/public")
@RequiredArgsConstructor
public class PublicContactController {
    private static final String DEFAULT_SUPER_ADMIN_EMAIL = "lengocdung8019@gmail.com";

    private final EmailService emailService;
    private final AccountRepository accountRepository;
    private final HotelRepository hotelRepository;

    public record ContactInquiryRequest(
            String name,
            String email,
            String phone,
            Long hotelId,
            String branchName,
            String subject,
            String message
    ) {}

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendContactInquiry(@RequestBody ContactInquiryRequest request) {
        if (request == null
                || request.name() == null || request.name().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Void>builder()
                            .code(400)
                            .message("Vui lòng điền đầy đủ họ tên, email và nội dung liên hệ.")
                            .build()
            );
        }

        String targetEmail = resolveRecipientEmail(request.hotelId(), request.branchName());

        emailService.sendContactInquiryEmail(
                request.name().trim(),
                request.email().trim(),
                request.phone() != null ? request.phone().trim() : "",
                request.branchName(),
                request.subject(),
                request.message().trim(),
                targetEmail
        );

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(1000)
                        .message("Đã gửi email góp ý thành công.")
                        .build()
        );
    }

    private String resolveRecipientEmail(Long hotelId, String branchName) {
        Long resolvedHotelId = hotelId;
        if (resolvedHotelId == null && branchName != null && !branchName.isBlank()) {
            String trimmedBranch = branchName.trim();
            if (!trimmedBranch.toLowerCase().contains("tất cả")
                    && !trimmedBranch.toLowerCase().contains("all branches")
                    && !trimmedBranch.toLowerCase().contains("toàn hệ thống")) {
                resolvedHotelId = hotelRepository.findAll().stream()
                        .filter(h -> h.getName() != null && h.getName().trim().equalsIgnoreCase(trimmedBranch))
                        .map(Hotel::getId)
                        .findFirst()
                        .orElse(null);
            }
        }

        if (resolvedHotelId != null) {
            List<String> managerEmails = accountRepository.findEmailsByHotelIdAndRoleCodeIn(
                    resolvedHotelId, List.of("ROLE_MANAGER", "ROLE_ADMIN"));
            if (managerEmails != null && !managerEmails.isEmpty()) {
                return managerEmails.get(0);
            }
            List<String> staffEmails = accountRepository.findEmailsByHotelIdAndRoleCodeIn(
                    resolvedHotelId, List.of("ROLE_RECEPTIONIST", "ROLE_STAFF"));
            if (staffEmails != null && !staffEmails.isEmpty()) {
                return staffEmails.get(0);
            }
        }

        List<String> superAdminEmails = accountRepository.findSuperAdminEmails();
        if (superAdminEmails != null && !superAdminEmails.isEmpty()) {
            return superAdminEmails.get(0);
        }
        return DEFAULT_SUPER_ADMIN_EMAIL;
    }
}
