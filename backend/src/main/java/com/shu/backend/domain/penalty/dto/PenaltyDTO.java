package com.shu.backend.domain.penalty.dto;

import com.shu.backend.domain.penalty.entity.Penalty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PenaltyDTO {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Getter
    @Builder
    public static class MyActiveResponse {
        private boolean penalized;
        private String expiresAt;  // ISO-8601 문자열, penalized=false면 null
        private String reason;     // penalized=false면 null
        private Long reportId;
    }

    @Getter
    @Builder
    public static class SummaryResponse {
        private Long penaltyId;
        private Long userId;
        private String userNickname;
        private Long reportId;
        private String reason;
        private String status;
        private String expiresAt;  // ISO-8601 문자열
        private String createdAt;  // ISO-8601 문자열

        public static SummaryResponse from(Penalty p) {
            return SummaryResponse.builder()
                    .penaltyId(p.getId())
                    .userId(p.getUser().getId())
                    .userNickname(p.getUser().getNickname())
                    .reportId(p.getReport().getId())
                    .reason(p.getReason().name())
                    .status(p.getStatus().name())
                    .expiresAt(p.getExpiresAt().format(ISO_FMT))
                    .createdAt(p.getCreatedAt().format(ISO_FMT))
                    .build();
        }
    }
}

