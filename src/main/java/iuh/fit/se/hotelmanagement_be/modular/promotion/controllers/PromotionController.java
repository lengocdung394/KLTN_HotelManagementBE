package iuh.fit.se.hotelmanagement_be.modular.promotion.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionStatus;
import iuh.fit.se.hotelmanagement_be.modular.promotion.enums.PromotionType;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.ChangeStatusRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.CreatePromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.requests.UpdatePromotionRequest;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PageResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.responses.PromotionResponse;
import iuh.fit.se.hotelmanagement_be.modular.promotion.services.PromotionService;
import iuh.fit.se.hotelmanagement_be.shared.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Promotion", description = "APIs quản lý khuyến mãi khách sạn SenViet")
public class PromotionController {

    PromotionService promotionService;

    // ============================================================
    // POST /promotions — Tạo mới
    // ============================================================
    @PostMapping
    @Operation(summary = "Tạo mới khuyến mãi")
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<PromotionResponse>builder()
                        .code(1000)
                        .result(promotionService.createPromotion(request))
                        .message("Tạo khuyến mãi thành công")
                        .build());
    }

    // ============================================================
    // GET /promotions/{id} — Tìm theo ID
    // ============================================================
    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết khuyến mãi theo ID")
    public ResponseEntity<ApiResponse<PromotionResponse>> getById(
            @Parameter(description = "ID của khuyến mãi") @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.<PromotionResponse>builder()
                .code(1000)
                .result(promotionService.getPromotionById(id))
                .message("Lấy thông tin thành công")
                .build());
    }

    // ============================================================
    // GET /promotions — Lấy tất cả (filter + pagination)
    // ============================================================
    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả khuyến mãi",
               description = "Hỗ trợ filter: status, type, keyword, startDate, endDate, page, size, sortBy, sortDir")
    public ResponseEntity<ApiResponse<PageResponse<PromotionResponse>>> getAll(
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(required = false) PromotionType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return ResponseEntity.ok(ApiResponse.<PageResponse<PromotionResponse>>builder()
                .code(1000)
                .result(promotionService.getAllPromotions(status, type, keyword, startDate, endDate,
                        PageRequest.of(page, size, sort)))
                .message("Lấy danh sách thành công")
                .build());
    }

    // ============================================================
    // GET /promotions/active — Danh sách đang hoạt động
    // ============================================================
    @GetMapping("/active")
    @Operation(summary = "Lấy danh sách khuyến mãi đang ACTIVE")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.<List<PromotionResponse>>builder()
                .code(1000)
                .result(promotionService.getActivePromotions())
                .message("Lấy danh sách thành công")
                .build());
    }

    // ============================================================
    // PUT /promotions/{id} — Cập nhật thông tin
    // ============================================================
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin khuyến mãi")
    public ResponseEntity<ApiResponse<PromotionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromotionRequest request) {

        return ResponseEntity.ok(ApiResponse.<PromotionResponse>builder()
                .code(1000)
                .result(promotionService.updatePromotion(id, request))
                .message("Cập nhật thành công")
                .build());
    }

    // ============================================================
    // PATCH /promotions/{id}/status — Đổi trạng thái
    // ============================================================
    @PatchMapping("/{id}/status")
    @Operation(summary = "Thay đổi trạng thái khuyến mãi",
               description = "DRAFT→ACTIVE/INACTIVE | ACTIVE→INACTIVE/EXPIRED | INACTIVE→ACTIVE/EXPIRED")
    public ResponseEntity<ApiResponse<PromotionResponse>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {

        return ResponseEntity.ok(ApiResponse.<PromotionResponse>builder()
                .code(1000)
                .result(promotionService.changeStatus(id, request))
                .message("Cập nhật trạng thái thành công")
                .build());
    }

    // ============================================================
    // DELETE /promotions/{id} — Xóa mềm
    // ============================================================
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa khuyến mãi (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(1000)
                .message("Xóa khuyến mãi thành công")
                .build());
    }
}
