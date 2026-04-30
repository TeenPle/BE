package com.shu.backend.domain.warning.dto;

import com.shu.backend.domain.warning.entity.Warning;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

public class WarningDTO {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Getter
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

        public static UnreadResponse from(Warning w) {
            return UnreadResponse.builder()
                    .warningId(w.getId())
                    .adminComment(w.getAdminComment())
                    .issuedAt(w.getCreatedAt().format(ISO_FMT))
                    .build();
        }
    }
}
