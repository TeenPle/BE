package com.shu.backend.domain.media.entity;

import com.shu.backend.domain.media.enums.MediaTargetType;
import com.shu.backend.domain.media.enums.MediaStatus;
import com.shu.backend.domain.media.enums.MediaType;
import com.shu.backend.domain.media.enums.ModerationStatus;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Media extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private MediaTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaStatus status = MediaStatus.ATTACHED;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 30)
    private ModerationStatus moderationStatus = ModerationStatus.APPROVED;

    @Column(name = "moderation_labels", columnDefinition = "TEXT")
    private String moderationLabels;

    private java.time.LocalDateTime deletedAt;

    private java.time.LocalDateTime storageDeletedAt;

    public static Media ofChatUpload(
            String url,
            String s3Key,
            MediaType mediaType,
            User uploader,
            String moderationLabels
    ) {
        Media m = new Media();
        m.url = url;
        m.s3Key = s3Key;
        m.mediaType = mediaType;
        m.uploader = uploader;
        m.status = MediaStatus.UPLOADED;
        m.moderationStatus = ModerationStatus.APPROVED;
        m.moderationLabels = moderationLabels;
        return m;
    }

    public static Media ofPost(String url, Long postId, MediaType mediaType, User uploader) {
        Media m = new Media();
        m.url = url;
        m.targetType = MediaTargetType.POST;
        m.targetId = postId;
        m.mediaType = mediaType;
        m.uploader = uploader;
        m.status = MediaStatus.ATTACHED;
        m.moderationStatus = ModerationStatus.APPROVED;
        return m;
    }

    public void attachToChatMessage(Long chatMessageId) {
        this.targetType = MediaTargetType.CHAT_MESSAGE;
        this.targetId = chatMessageId;
        this.status = MediaStatus.ATTACHED;
    }

    public boolean isApprovedChatUploadBy(Long uploaderId) {
        return this.status == MediaStatus.UPLOADED
                && this.moderationStatus == ModerationStatus.APPROVED
                && this.targetType == null
                && this.targetId == null
                && this.uploader != null
                && this.uploader.getId().equals(uploaderId);
    }
}
