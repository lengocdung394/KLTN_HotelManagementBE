package iuh.fit.se.hotelmanagement_be.modular.branch.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.services.impl.EmailService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contact/public")
@RequiredArgsConstructor
public class PublicContactController {
    private final EmailService emailService;

    public record ContactInquiryRequest(
            String name,
            String email,
            String phone,
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

        emailService.sendContactInquiryEmail(
                request.name().trim(),
                request.email().trim(),
                request.phone() != null ? request.phone().trim() : "",
                request.branchName(),
                request.subject(),
                request.message().trim()
        );

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(1000)
                        .message("Đã gửi email góp ý thành công.")
                        .build()
        );
    }
}
