package iuh.fit.se.hotelmanagement_be.modular.report.controllers;

import iuh.fit.se.hotelmanagement_be.modular.auth.entities.Account;
import iuh.fit.se.hotelmanagement_be.modular.branch.repositories.HotelRepository;
import iuh.fit.se.hotelmanagement_be.modular.report.responses.HotelReportResponse;
import iuh.fit.se.hotelmanagement_be.modular.report.services.HotelReportService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class ReportController {
    private final HotelReportService hotelReportService;
    private final HotelRepository hotelRepository;

    @GetMapping("/hotel-summary")
    public ResponseEntity<ApiResponse<HotelReportResponse>> getHotelReport(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long hotelId,
            Authentication authentication
    ) {
        LocalDate effectiveEndDate = endDate == null ? LocalDate.now() : endDate;
        LocalDate effectiveStartDate = startDate == null
                ? effectiveEndDate.withDayOfMonth(1)
                : startDate;
        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate.");
        }

        Account account = (Account) authentication.getPrincipal();
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
        Long reportHotelId = account.getHotelId();
        if (isSuperAdmin) {
            if (hotelId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hotelId is required for super admin reports.");
            }
            reportHotelId = hotelId;
        } else if (reportHotelId == null || (hotelId != null && !hotelId.equals(reportHotelId))) {
            throw new AccessDeniedException("You can only view reports for your assigned hotel.");
        }
        if (!hotelRepository.existsById(reportHotelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel was not found.");
        }

        HotelReportResponse report = hotelReportService.getHotelReport(
                reportHotelId,
                effectiveStartDate,
                effectiveEndDate
        );
        return ResponseEntity.ok(ApiResponse.<HotelReportResponse>builder()
                .code(200)
                .message("Lấy báo cáo khách sạn thành công.")
                .result(report)
                .build());
    }
}
