package com.shu.backend.domain.usersetting.dto;

import com.shu.backend.domain.usersetting.entity.UserSetting;
import lombok.Builder;
import lombok.Getter;

public class UserSettingDTO {

    @Getter
    @Builder
    public static class Response {
        private boolean allowPush;
        private boolean allowCommentNotification;
        private boolean allowReplyNotification;
        private boolean allowLikeNotification;
        private boolean allowChatNotification;

        public static Response from(UserSetting s) {
            return Response.builder()
                    .allowPush(Boolean.TRUE.equals(s.getAllowPush()))
                    .allowCommentNotification(Boolean.TRUE.equals(s.getAllowCommentNotification()))
                    .allowReplyNotification(Boolean.TRUE.equals(s.getAllowReplyNotification()))
                    .allowLikeNotification(Boolean.TRUE.equals(s.getAllowLikeNotification()))
                    .allowChatNotification(Boolean.TRUE.equals(s.getAllowChatNotification()))
                    .build();
        }
    }

    @Getter
    public static class UpdateRequest {
        // null이면 변경하지 않음
        private Boolean allowPush;
        private Boolean allowCommentNotification;
        private Boolean allowReplyNotification;
        private Boolean allowLikeNotification;
        private Boolean allowChatNotification;
    }
}
