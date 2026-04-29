package com.shu.backend.domain.chatroom.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ChatRoomDTO {

    @Getter @Setter
    public static class CreateDmRequest {
        private Long otherUserId;
        private Long sourcePostId;   // 채팅 유입 게시글 ID
        private String roomTitle;    // 게시글 제목 (채팅방 표시 이름으로 사용)
    }

    @Getter @Builder
    public static class CreateDmResponse {
        private Long roomId;
        private Long otherUserId;
        private String displayName;
        private LocalDateTime lastMessageAt;
        private boolean blocked;
        private boolean blockedByMe;
        private boolean blockedByOther;
    }

    @Getter @Builder
    public static class RoomListItem {
        private Long roomId;
        private Long otherUserId;

        private String lastPreview;
        private LocalDateTime lastMessageAt;

        private String displayName;

        private long unreadCount;
        private boolean blocked;
        private boolean blockedByMe;
        private boolean blockedByOther;
    }

    @Getter @Builder
    public static class RoomListResponse {
        private List<RoomListItem> rooms;
    }

    @Getter @Setter
    public static class ReportRequest {
        private String reason;
    }
}
