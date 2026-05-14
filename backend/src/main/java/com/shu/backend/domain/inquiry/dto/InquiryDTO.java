package com.shu.backend.domain.inquiry.dto;

import com.shu.backend.domain.inquiry.entity.Inquiry;
import com.shu.backend.domain.inquiry.enums.InquiryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class InquiryDTO {

    @Getter
    public static class CreateRequest {
        @NotBlank
        @Size(max = 100)
        private String title;

        @NotBlank
        @Size(max = 2000)
        private String content;
    }

    @Getter
    public static class AnswerRequest {
        @NotBlank
        @Size(max = 2000)
        private String answer;
    }

    @Getter
    @Builder
    public static class SummaryResponse {
        private Long inquiryId;
        private String title;
        private InquiryStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime answeredAt;
        private Long userId;
        private String userName;
        private String userNickname;
        private String schoolName;

        public static SummaryResponse from(Inquiry inquiry) {
            return SummaryResponse.builder()
                    .inquiryId(inquiry.getId())
                    .title(inquiry.getTitle())
                    .status(inquiry.getStatus())
                    .createdAt(inquiry.getCreatedAt())
                    .answeredAt(inquiry.getAnsweredAt())
                    .userId(inquiry.getUser().getId())
                    .userName(inquiry.getUser().getUsername())
                    .userNickname(inquiry.getUser().getNickname())
                    .schoolName(inquiry.getUser().getSchool().getName())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class DetailResponse {
        private Long inquiryId;
        private String title;
        private String content;
        private InquiryStatus status;
        private String adminAnswer;
        private LocalDateTime createdAt;
        private LocalDateTime answeredAt;
        private Long userId;
        private String userName;
        private String userNickname;
        private String schoolName;

        public static DetailResponse from(Inquiry inquiry) {
            return DetailResponse.builder()
                    .inquiryId(inquiry.getId())
                    .title(inquiry.getTitle())
                    .content(inquiry.getContent())
                    .status(inquiry.getStatus())
                    .adminAnswer(inquiry.getAdminAnswer())
                    .createdAt(inquiry.getCreatedAt())
                    .answeredAt(inquiry.getAnsweredAt())
                    .userId(inquiry.getUser().getId())
                    .userName(inquiry.getUser().getUsername())
                    .userNickname(inquiry.getUser().getNickname())
                    .schoolName(inquiry.getUser().getSchool().getName())
                    .build();
        }
    }
}
