package com.shu.backend.domain.warning.controller;

import com.shu.backend.domain.warning.dto.WarningDTO;
import com.shu.backend.domain.warning.service.WarningService;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Warning", description = "관리자 경고 조회 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/warnings")
public class AdminWarningController {

    private final WarningService warningService;

    @Operation(summary = "전체 경고 목록 조회")
    @GetMapping
    public ApiResponse<Page<WarningDTO.HistoryResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.onSuccess(warningService.getAllWarnings(pageable));
    }

    @Operation(summary = "유저별 경고 목록 조회")
    @GetMapping("/user")
    public ApiResponse<Page<WarningDTO.HistoryResponse>> listByUser(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.onSuccess(warningService.getWarningsByUser(userId, pageable));
    }
}
