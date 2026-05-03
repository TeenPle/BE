package com.shu.backend.domain.report.dto;

import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.enums.ReportStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

/**
 * 관리자 신고목록 조회용 DTO
 */
@Getter
@Builder
public class ReportSummaryResponse {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Long reportId;
    private Long reporterId;
    private String reporterNickname;
    private Long reportedUserId;
    private String reportedUserNickname;
    private String targetType;
    private Long targetId;
    private String reportReason;
    private String status;
    private String createdAt;
    private String processedAt;

    public static ReportSummaryResponse from(Report r) {
        return ReportSummaryResponse.builder()
                .reportId(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterNickname(r.getReporter().getNickname())
                .reportedUserId(r.getReportedUser().getId())
                .reportedUserNickname(r.getReportedUser().getNickname())
                .targetType(r.getTargetType().name())
                .targetId(r.getTargetId())
                .reportReason(r.getReportReason().name())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().format(ISO_FMT) : null)
                .processedAt(r.getProcessedAt() != null ? r.getProcessedAt().format(ISO_FMT) : null)
                .build();
    }

    /** 관리자 신고 상세 조회용 DTO (신고된 내용 포함) */
    @Getter
    @Builder
    public static class DetailResponse {
        private Long reportId;
        private Long reporterId;
        private String reporterNickname;
        private Long reportedUserId;
        private String reportedUserNickname;
        private String targetType;
        private Long targetId;
        private String targetContent;
        private String schoolName;
        private String boardTitle;
        private String reportReason;
        private String status;
        private String createdAt;
        private String processedAt;
    }
}
