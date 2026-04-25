package com.shu.backend.domain.usersetting.entity;


import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_setting",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_setting_user",
                        columnNames = {"user_id"}
                )
        }
)
public class UserSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 전체 푸시 알람
    @Column(name = "allow_push", nullable = false)
    @Builder.Default
    private Boolean allowPush = true;

    // 댓글 알림
    @Column(name = "allow_comment_notification", nullable = false)
    @Builder.Default
    private Boolean allowCommentNotification = true;

    // 대댓글 알림
    @Column(name = "allow_reply_notification", nullable = false)
    @Builder.Default
    private Boolean allowReplyNotification = true;

    // 좋아요 알림
    @Column(name = "allow_like_notification", nullable = false)
    @Builder.Default
    private Boolean allowLikeNotification = true;

    // 실시간 채팅 알림
    @Column(name = "allow_chat_notification", nullable = false)
    @Builder.Default
    private Boolean allowChatNotification = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    //=== 생성 메서드 ===//
    public static UserSetting create(User user) {
        // @Builder.Default는 builder()를 통해서만 기본값이 적용되므로 no-args 생성자 대신 builder 사용
        return UserSetting.builder()
                .user(user)
                .allowPush(true)
                .allowCommentNotification(true)
                .allowReplyNotification(true)
                .allowLikeNotification(true)
                .allowChatNotification(true)
                .build();
    }

    //=== 업데이트(부분 업데이트 지원) ===//
    public void update(Boolean allowPush,
                       Boolean allowCommentNotification,
                       Boolean allowReplyNotification,
                       Boolean allowLikeNotification,
                       Boolean allowChatNotification) {

        if (allowPush != null) this.allowPush = allowPush;
        if (allowCommentNotification != null) this.allowCommentNotification = allowCommentNotification;
        if (allowReplyNotification != null) this.allowReplyNotification = allowReplyNotification;
        if (allowLikeNotification != null) this.allowLikeNotification = allowLikeNotification;
        if (allowChatNotification != null) this.allowChatNotification = allowChatNotification;

        normalize();
    }

    private void normalize() {
        if (Boolean.FALSE.equals(this.allowPush)) {
            this.allowCommentNotification = false;
            this.allowReplyNotification = false;
            this.allowLikeNotification = false;
            this.allowChatNotification = false;
        }
    }

    public boolean isCommentNotificationEnabled() {
        return Boolean.TRUE.equals(allowPush) && Boolean.TRUE.equals(allowCommentNotification);
    }
    public boolean isReplyNotificationEnabled() {
        return Boolean.TRUE.equals(allowPush) && Boolean.TRUE.equals(allowReplyNotification);
    }
    public boolean isLikeNotificationEnabled() {
        return Boolean.TRUE.equals(allowPush) && Boolean.TRUE.equals(allowLikeNotification);
    }
    public boolean isChatNotificationEnabled() {
        return Boolean.TRUE.equals(allowPush) && Boolean.TRUE.equals(allowChatNotification);
    }
}
