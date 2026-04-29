package com.shu.backend.domain.chatroom.entity;

import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "chat_room",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_room_dm_source",
                columnNames = {"user1_id", "user2_id", "source_post_id"}
        ),
        indexes = {
                @Index(name = "idx_chat_room_last_message_at", columnList = "last_message_at"),
                @Index(name = "idx_room_empty_cutoff", columnList = "last_message_id, created_at")
        }
)
public class ChatRoom extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 채팅방 제목 (유입된 게시글 제목)
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    // 1:1 DM 페어 (정규화: user1Id = min, user2Id = max)
    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    // 채팅 유입 게시글 ID (같은 두 사용자라도 글이 다르면 별도 채팅방)
    @Column(name = "source_post_id", nullable = false)
    private Long sourcePostId;

    // 방 목록 성능용 요약 필드
    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Builder
    private ChatRoom(Long user1Id, Long user2Id, Long sourcePostId, String displayName) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.sourcePostId = sourcePostId;
        this.displayName = displayName;
    }

    public static ChatRoom ofDm(Long a, Long b, Long sourcePostId, String title) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return ChatRoom.builder()
                .user1Id(min)
                .user2Id(max)
                .sourcePostId(sourcePostId)
                .displayName(title)
                .build();
    }

    public void updateLastMessage(Long messageId, LocalDateTime messageAt) {
        this.lastMessageId = messageId;
        this.lastMessageAt = messageAt;
    }

    public void rename(String name) {
        this.displayName = name;
    }
}
