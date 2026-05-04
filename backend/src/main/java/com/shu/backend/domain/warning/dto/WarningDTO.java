package com.shu.backend.domain.warning.dto;

import com.shu.backend.domain.report.enums.TargetType;
import com.shu.backend.domain.warning.entity.Warning;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

public class WarningDTO {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueRequest {
        @NotBlank(message = "관리자 코멘트를 입력해주세요.")
        @Size(max = 500, message = "코멘트는 500자 이하로 입력해주세요.")
        private String adminComment;
    }

    /** 유저에게 내려주는 미확인 경고 응답 */
    @Getter
    @Builder
    public static class UnreadResponse {
        private Long warningId;
        private String adminComment;
        private String issuedAt;  // ISO-8601
        private String targetType;    // POST | COMMENT
        private String targetSummary; // 신고된 콘텐츠 요약 (최대 80자)

        public static UnreadResponse from(Warning w) {
            return UnreadResponse.builder()
                    .warningId(w.getId())
                    .adminComment(w.getAdminComment())
                    .issuedAt(w.getCreatedAt().format(ISO_FMT))
                    .build();
        }

        public static UnreadResponse from(Warning w, String targetType, String targetSummary) {
            return UnreadResponse.builder()
                    .warningId(w.getId())
                    .adminComment(w.getAdminComment())
                    .issuedAt(w.getCreatedAt().format(ISO_FMT))
                    .targetType(targetType)
                    .targetSummary(targetSummary)
                    .build();
        }
    }

    /** 경고 이력 항목 (유저 본인 조회 + 관리자 조회 공용) */
    @Getter
    @Builder
    public static class HistoryResponse {
        private Long warningId;
        private Long userId;
        private String userNickname;
        private Long reportId;
        private String targetType;
        private String targetSummary;
        private String adminComment;
        private Boolean isRead;
        private String issuedAt;

        public static HistoryResponse from(Warning w, String targetSummary) {
            TargetType tt = w.getReport().getTargetType();
            return HistoryResponse.builder()
                    .warningId(w.getId())
                    .userId(w.getUser().getId())
                    .userNickname(w.getUser().getNickname())
                    .reportId(w.getReport().getId())
                    .targetType(tt != null ? tt.name() : null)
                    .targetSummary(targetSummary)
                    .adminComment(w.getAdminComment())
                    .isRead(w.getIsRead())
                    .issuedAt(w.getCreatedAt().format(ISO_FMT))
                    .build();
        }
    }
}
