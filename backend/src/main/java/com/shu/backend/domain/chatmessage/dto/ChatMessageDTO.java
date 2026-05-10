package com.shu.backend.domain.chatmessage.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageDTO {

    public enum MessageType { TEXT, IMAGE }

    // =================== 전송 ===================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SendRequest {
        private Long roomId;
        private MessageType type;
        private String content;   // TEXT
        private Long mediaId;     // IMAGE
        private String imageUrl;  // IMAGE
    }

    // =================== 업로드 응답 ===================
    @Getter
    @AllArgsConstructor
    public static class UploadImageResponse {
        private Long mediaId;
        private String imageUrl;
    }

    // =================== 메시지 응답 ===================
    @Getter
    @Builder
    public static class MessageResponse {
        private Long messageId;
        private Long roomId;
        private Long senderId;

        private MessageType type;
        private String content;
        private List<MediaItem> medias;

        private LocalDateTime createdAt;
    }

    // =================== 메시지 생성 이벤트 (STOMP 브로드캐스트용) ===================
    @Getter
    @Builder
    public static class MessageCreatedBroadcast {
        private String eventType; // "MESSAGE_CREATED" 고정
        private MessageResponse message;
    }

    // =================== 미디어 ===================
    @Getter
    @Builder
    public static class MediaItem {
        private Long id;
        private String url;
        private String mediaType;
    }

    // =================== 메시지 목록 ===================
    @Getter
    @Builder
    public static class MessageListResponse {
        private Long roomId;
        private List<MessageResponse> messages;
        // 상대방이 마지막으로 읽은 메시지 ID (카카오톡 "1" 표시 기준)
        private Long otherLastReadMessageId;
        // 내가 차단했거나 상대가 나를 차단한 상태면 입력창을 잠근다.
        private boolean blocked;
        private boolean blockedByMe;
        private boolean blockedByOther;
        private boolean otherUserDeleted;
        private boolean canSendMessage;
        private boolean canReport;
        private boolean canBlock;
    }

    // =================== 읽음 영수증 (STOMP 브로드캐스트용) ===================
    @Getter
    @Builder
    public static class ReadReceiptBroadcast {
        private String eventType; // "READ_RECEIPT" 고정
        private String type; // "READ_RECEIPT" 고정
        private Long readerId;
        private Long lastReadMessageId;
    }

    // =================== 전송 실패 알림 (STOMP 브로드캐스트용) ===================
    @Getter
    @Builder
    public static class SendErrorBroadcast {
        private String eventType; // "SEND_ERROR" 고정
        private String type; // "SEND_ERROR" 고정
        private Long senderId;
        private String code;
        private String message;
    }

    // =================== 채팅방 목록 갱신 알림 (STOMP 브로드캐스트용) ===================
    @Getter
    @Builder
    public static class RoomUpdatedBroadcast {
        private String eventType; // "ROOM_LIST_UPDATED" 또는 "ROOM_STATE_UPDATED"
        private String type; // 기존 클라이언트 호환용
        private Long roomId;
    }

    // =================== 읽음 처리 ===================
    @Getter
    @Setter
    public static class ReadRequest {
        private Long messageId;
    }
}
