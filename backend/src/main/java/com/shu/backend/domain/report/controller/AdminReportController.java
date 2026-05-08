package com.shu.backend.domain.report.controller;

import com.shu.backend.domain.report.dto.ReportAdminDTO;
import com.shu.backend.domain.report.dto.ReportDTO;
import com.shu.backend.domain.report.dto.ReportSummaryResponse;
import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.enums.ReportStatus;
import com.shu.backend.domain.report.exception.status.ReportSuccessStatus;
import com.shu.backend.domain.report.service.ReportService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.warning.dto.WarningDTO;
import com.shu.backend.domain.warning.service.WarningService;
import com.shu.backend.global.apiPayload.ApiResponse;
import com.shu.backend.global.util.PageRequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Report", description = "관리자 신고 처리 관련 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reportService;
    private final WarningService warningService;

    @Operation(
            summary = "신고 목록 조회",
            description = "상태별(PENDING/RESOLVED/REJECTED) 신고 목록을 페이지 단위로 조회합니다."
    )
    @GetMapping
    public ApiResponse<Page<ReportSummaryResponse>> getReports(
            @RequestParam(defaultValue = "PENDING") ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        Pageable pageable = PageRequestUtils.of(page, size, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reports = reportService.getReports(status, pageable);

        return ApiResponse.of(
                ReportSuccessStatus.REPORT_LIST_SUCCESS,
                reports.map(ReportSummaryResponse::from));

    }

    @Operation(
            summary = "신고 상세 조회",
            description = "신고된 게시글/댓글 내용, 신고자/피신고자 닉네임 포함 상세 정보를 반환합니다."
    )
    @GetMapping("/{reportId}")
    public ApiResponse<ReportSummaryResponse.DetailResponse> getReportDetail(
            @PathVariable Long reportId
    ) {
        return ApiResponse.of(
                ReportSuccessStatus.REPORT_LIST_SUCCESS,
                reportService.getReportDetail(reportId));
    }

    @Operation(
            summary = "신고 승인(제재 생성)",
            description = """
                    신고를 RESOLVED 처리하고, penaltyDays 기간의 제재를 생성합니다.
                    - 제재는 신고 승인 시에만 생성됩니다.
                    - 신고 1건당 제재 1건(중복 생성 방지)
                    """
    )
    @PostMapping("/{reportId}/approve")
    public ApiResponse<Long> approveReport(
            @AuthenticationPrincipal User admin,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportAdminDTO.ApproveRequest req
            ) {
        Long penaltyId = reportService.approve(admin.getId(), reportId, req.getPenaltyDays(), req.getAdminComment());
        return ApiResponse.of(ReportSuccessStatus.REPORT_APPROVE_SUCCESS, penaltyId);
    }

    @Operation(
            summary = "신고 거절",
            description = "신고를 REJECTED 처리합니다. 거절 시 제재는 생성되지 않습니다."
    )
    @PostMapping("/{reportId}/reject")
    public ApiResponse<Long> rejectReport(
            @AuthenticationPrincipal User admin,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportAdminDTO.RejectRequest req
    ) {
        Long id = reportService.reject(admin.getId(), reportId, req.getAdminComment());
        return ApiResponse.of(ReportSuccessStatus.REPORT_REJECT_SUCCESS, id);
    }

    @Operation(
            summary = "경고 발령",
            description = "신고를 WARNED 처리하고 피신고자에게 경고를 발령합니다. 관리자 코멘트가 경고 팝업에 노출됩니다."
    )
    @PostMapping("/{reportId}/warn")
    public ApiResponse<Long> warnReport(
            @AuthenticationPrincipal User admin,
            @PathVariable Long reportId,
            @Valid @RequestBody WarningDTO.IssueRequest req
    ) {
        Long warningId = warningService.issue(admin.getId(), reportId, req.getAdminComment());
        return ApiResponse.onSuccess(warningId);
    }
}
