package com.shu.backend.domain.boardprofile.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shu.backend.domain.boardprofile.entity.BoardDisplayProfile;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

public class BoardDisplayProfileDTO {

    @Getter
    @Builder
    public static class Response {
        private Long boardId;
        private String boardName;
        private String displayName;
        private String profileImageUrl;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastChangedAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime nextChangeAvailableAt;
        private boolean changeAvailable;
        private long remainingDays;

        public static Response from(BoardDisplayProfile profile, String profileImageUrl, LocalDateTime now) {
            LocalDateTime next = profile.getNextChangeAvailableAt();
            boolean available = next == null || !next.isAfter(now);
            long remainingDays = 0;
            if (!available) {
                remainingDays = Math.max(1, Duration.between(now, next).toDays() + 1);
            }
            return Response.builder()
                    .boardId(profile.getBoard().getId())
                    .boardName(profile.getBoard().getTitle())
                    .displayName(profile.getDisplayName())
                    .profileImageUrl(profileImageUrl)
                    .lastChangedAt(profile.getLastChangedAt())
                    .nextChangeAvailableAt(next)
                    .changeAvailable(available)
                    .remainingDays(remainingDays)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String displayName;
    }
}
