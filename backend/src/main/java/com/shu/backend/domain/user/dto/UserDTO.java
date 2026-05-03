package com.shu.backend.domain.user.dto;

import com.shu.backend.domain.user.enums.Gender;
import com.shu.backend.domain.user.enums.Grade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserDTO {

    @Getter
    @Builder
    public static class ProfileResponse {
        private Long id;
        private String nickname;
        private String email;
        private String profileImageUrl;
        private String schoolName;
        private Grade grade;
        private Gender gender;
        private boolean verified;
        private boolean phoneVerified;
        private long myPostCount;
        private long myCommentCount;
        private LocalDateTime nicknameChangedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NicknameUpdateRequest {
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다.")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordUpdateRequest {
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        private String currentPassword;

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        private String newPassword;
    }

    @Getter
    @Builder
    public static class MyPostResponse {
        private Long postId;
        private String title;
        private String content;
        private String postStatus;
        private int likeCount;
        private int commentCount;
        private LocalDateTime createdAt;
        private String boardTitle;
    }

    @Getter
    @Builder
    public static class MyCommentResponse {
        private Long commentId;
        private String content;
        private Long postId;
        private String postTitle;
        private int likeCount;
        private LocalDateTime createdAt;
        private String boardTitle;
    }
}
