package com.shu.backend.domain.adminaudit.controller;

import com.shu.backend.domain.adminaudit.dto.AdminAuditLogResponse;
import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.adminaudit.service.AdminAuditLogService;
import com.shu.backend.global.apiPayload.ApiResponse;
import com.shu.backend.global.util.PageRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    @GetMapping
    public ApiResponse<Page<AdminAuditLogResponse>> getLogs(
            @RequestParam(required = false) AdminAuditAction action,
            @RequestParam(required = false) AdminAuditTargetType targetType,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequestUtils.of(page, size, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toExclusive = to != null ? to.plusDays(1).atStartOfDay() : null;
        return ApiResponse.onSuccess(adminAuditLogService.getLogs(
                action,
                targetType,
                adminId,
                fromDateTime,
                toExclusive,
                pageable
        ));
    }
}
