package com.shu.backend.domain.chatmessage.service;

import com.shu.backend.domain.chatmessage.dto.ChatMessageDTO;
import com.shu.backend.domain.chatmessage.entity.ChatMessage;
import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.chatmessage.repository.ChatMessageRepository;
import com.shu.backend.domain.chatroom.entity.ChatRoom;
import com.shu.backend.domain.chatroom.repository.ChatRoomRepository;
import com.shu.backend.domain.chatroomuser.entity.ChatRoomUser;
import com.shu.backend.domain.chatroomuser.repository.ChatRoomUserRepository;
import com.shu.backend.domain.media.entity.Media;
import com.shu.backend.domain.media.repository.MediaRepository;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.notification.service.NotificationService;
import com.shu.backend.domain.push.service.PushService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.usersetting.repository.UserSettingRepository;
import com.shu.backend.global.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 채팅 메시지 전송, 조회, 읽음 처리와
 * 이미지/알림/푸시 연동까지 담당하는 서비스.
 *
 * 메시지 저장과 채팅방 상태 갱신을 처리하고,
 * 필요 시 미디어 정보와 알림 발송까지 함께 수행한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MESSAGE_RATE_LIMIT = 20;
    private static final int MESSAGE_RATE_WINDOW_SECONDS = 60;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;

    private final NotificationService notificationService;
    private final PushService pushService;
    private final UserSettingRepository userSettingRepository;
    private final ChatActionRateLimiter chatActionRateLimiter;
    private final FileStorageService fileStorageService;

    // 채팅 메시지 전송
    @PreAuthorize("@penaltyChecker.notPenalized(#senderId)")
    public ChatMessageDTO.MessageResponse send(Long senderId, ChatMessageDTO.SendRequest req) {
        chatActionRateLimiter.check(senderId, "message", MESSAGE_RATE_LIMIT, MESSAGE_RATE_WINDOW_SECONDS);

        // 채팅방 조회 (없으면 예외)
        ChatRoom room = chatRoomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.CHAT_ROOM_NOT_FOUND));

        // 발신자 참여자 검증 (참여자가 아니면 예외)
        ChatRoomUser senderCru = chatRoomUserRepository.findByChatRoomIdAndUserId(room.getId(), senderId)
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.NOT_ROOM_MEMBER));

        // 1:1 DM에서 수신자 ID 계산
        Long receiverId = room.getUser1Id().equals(senderId) ? room.getUser2Id() : room.getUser1Id();

        // 수신자 참여자 정보 조회 (없으면 예외)
        ChatRoomUser receiverCru = chatRoomUserRepository.findByChatRoomIdAndUserId(room.getId(), receiverId)
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.NOT_ROOM_MEMBER));

        // 차단 상태면 메시지 전송 불가
        if (senderCru.isBlocked() || receiverCru.isBlocked()) {
            throw new ChatMessageException(ChatMessageErrorStatus.CHAT_BLOCKED);
        }

        // 수신자가 나갔거나 숨김이면, 상대가 메시지 보낼 때 자동 복귀 처리
        receiverCru.rejoinIfLeftOrHidden();
        senderCru.rejoinIfLeftOrHidden();

        // 발신자 User 레퍼런스 조회(프록시)
        User sender = userRepository.getReferenceById(senderId);

        // 요청 타입(TEXT/IMAGE) 변환 및 검증
        ChatMessage.MessageType type = mapType(req.getType());

        if (type == ChatMessage.MessageType.TEXT) {
            if (req.getContent() == null || req.getContent().isBlank()) {
                throw new ChatMessageException(ChatMessageErrorStatus.INVALID_MESSAGE_TYPE);
            }
            if (req.getContent().length() > MAX_TEXT_LENGTH) {
                throw new ChatMessageException(ChatMessageErrorStatus.MESSAGE_TOO_LONG);
            }
        }

        // IMAGE 메시지는 업로드/검수 완료된 mediaId 필수
        if (type == ChatMessage.MessageType.IMAGE) {
            if (req.getMediaId() == null) {
                throw new ChatMessageException(ChatMessageErrorStatus.IMAGE_MEDIA_REQUIRED);
            }
        }

        // 메시지 엔티티 생성 (TEXT만 content 저장, IMAGE는 content 비워둠)
        ChatMessage message = ChatMessage.builder()
                .type(type)
                .content(type == ChatMessage.MessageType.TEXT ? req.getContent() : null)
                .chatRoom(room)
                .sender(sender)
                .build();

        // 메시지 저장
        ChatMessage saved = chatMessageRepository.save(message);

        // IMAGE 메시지면 Media에 연결 저장 (targetType=CHAT_MESSAGE, targetId=messageId)
        ChatMessageDTO.MediaItem mediaItem = null;
        if (type == ChatMessage.MessageType.IMAGE) {
            Media savedMedia = mediaRepository.findByIdAndUploaderId(req.getMediaId(), senderId)
                    .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.CHAT_IMAGE_NOT_FOUND));

            if (!savedMedia.isApprovedChatUploadBy(senderId)) {
                throw new ChatMessageException(ChatMessageErrorStatus.CHAT_IMAGE_NOT_FOUND);
            }

            savedMedia.attachToChatMessage(saved.getId());

            mediaItem = ChatMessageDTO.MediaItem.builder()
                    .id(savedMedia.getId())
                    .url(fileStorageService.toPresignedReadUrl(savedMedia.getUrl()))
                    .mediaType(savedMedia.getMediaType().name())
                    .build();
        }

        // 채팅방 요약 필드 갱신 (방 목록 최신순 정렬/미리보기용)
        String preview = (type == ChatMessage.MessageType.TEXT)
                ? summarize(req.getContent(), 50)
                : "사진을 보냈습니다.";
        room.updateLastMessage(saved.getId(), saved.getCreatedAt(), preview);

        String notiMsg = (type == ChatMessage.MessageType.TEXT)
                ? "새 메시지: " + summarize(req.getContent(), 20)
                : "사진을 보냈습니다.";


        //새로운 채팅 알림 생성
        Long notificationId = notificationService.create(
                NotificationType.CHAT,
                NotificationTargetType.CHAT_MSG,
                room.getId(),
                notiMsg,
                receiverId,
                senderId
        );

        if (notificationId != null) {
            var setting = userSettingRepository.findByUserId(receiverId).orElse(null);
            // setting이 없으면 기본값(모든 알림 허용)으로 간주
            if (setting == null || setting.isChatNotificationEnabled()) {
                try {
                    pushService.sendToUserAfterCommit(
                            receiverId,
                            "새 채팅",
                            notiMsg,
                            Map.of(
                                    "notificationId", String.valueOf(notificationId),
                                    "type", NotificationType.CHAT.name(),
                                    "targetType", NotificationTargetType.CHAT_MSG.name(),
                                    "targetId", String.valueOf(room.getId())
                            )
                    );
                } catch (Exception ignore) {}
            }
        }

        // 응답 DTO 반환
        return ChatMessageDTO.MessageResponse.builder()
                .messageId(saved.getId())
                .roomId(room.getId())
                .senderId(senderId)
                .type(req.getType())
                .content(saved.getContent())
                .medias(mediaItem == null ? Collections.emptyList() : List.of(mediaItem))
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ChatMessageDTO.MessageListResponse getMessages(Long myId, Long roomId, Long lastId) {

        ChatRoomUser myCru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.NOT_ROOM_MEMBER));

        // 상대방 ID 계산 (1:1 DM 기준)
        ChatRoom room = myCru.getChatRoom();
        Long otherId = room.getUser1Id().equals(myId) ? room.getUser2Id() : room.getUser1Id();

        // 상대방이 마지막으로 읽은 메시지 ID (카카오톡 "1" 기준값)
        Long otherLastRead = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, otherId)
                .map(ChatRoomUser::getLastReadMessageId)
                .orElse(null);
        boolean blockedByOther = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, otherId)
                .map(ChatRoomUser::isBlocked)
                .orElse(false);

        List<ChatMessage> list = (lastId == null)
                ? chatMessageRepository.findTop50ByChatRoomIdOrderByIdDesc(roomId)
                : chatMessageRepository.findTop50ByChatRoomIdAndIdLessThanOrderByIdDesc(roomId, lastId);

        // messageIds 추출
        List<Long> messageIds = list.stream().map(ChatMessage::getId).toList();

        // Media 한번에 조회 후 targetId(messageId)로 그룹핑
        var mediaMap = messageIds.isEmpty()
                ? java.util.Collections.<Long, List<Media>>emptyMap()
                : mediaRepository.findByTargetTypeAndTargetIdIn(
                com.shu.backend.domain.media.enums.MediaTargetType.CHAT_MESSAGE,
                messageIds
        ).stream().collect(java.util.stream.Collectors.groupingBy(Media::getTargetId));

        List<ChatMessageDTO.MessageResponse> res = list.stream().map(m -> {
            List<ChatMessageDTO.MediaItem> mediaItems =
                    mediaMap.getOrDefault(m.getId(), java.util.Collections.emptyList())
                            .stream()
                            .map(md -> ChatMessageDTO.MediaItem.builder()
                                    .id(md.getId())
                                    .url(fileStorageService.toPresignedReadUrl(md.getUrl()))
                                    .mediaType(md.getMediaType().name())
                                    .build())
                            .toList();

            return ChatMessageDTO.MessageResponse.builder()
                    .messageId(m.getId())
                    .roomId(roomId)
                    .senderId(m.getSender().getId())
                    .type(ChatMessageDTO.MessageType.valueOf(m.getType().name()))
                    .content(m.getContent())
                    .medias(mediaItems)
                    .createdAt(m.getCreatedAt())
                    .build();
        }).toList();

        return ChatMessageDTO.MessageListResponse.builder()
                .roomId(roomId)
                .messages(res)
                .otherLastReadMessageId(otherLastRead)
                .blocked(myCru.isBlocked() || blockedByOther)
                .blockedByMe(myCru.isBlocked())
                .blockedByOther(blockedByOther)
                .build();
    }

    public void read(Long myId, Long roomId, Long messageId) {
        if (messageId == null) {
            throw new ChatMessageException(ChatMessageErrorStatus.INVALID_READ_MESSAGE);
        }

        // 참여자 조회 (참여자가 아니면 예외)
        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.NOT_ROOM_MEMBER));

        if (!chatMessageRepository.existsByIdAndChatRoomId(messageId, roomId)) {
            throw new ChatMessageException(ChatMessageErrorStatus.INVALID_READ_MESSAGE);
        }

        // 읽음 처리 (lastReadMessageId 최신화)
        cru.read(messageId);
    }

    private ChatMessage.MessageType mapType(ChatMessageDTO.MessageType type) {

        // 타입이 null이면 예외
        if (type == null) throw new ChatMessageException(ChatMessageErrorStatus.INVALID_MESSAGE_TYPE);

        // DTO 타입을 엔티티 enum 타입으로 변환
        return ChatMessage.MessageType.valueOf(type.name());
    }

    public static String summarize(String content, int maxLength) {
        if (content == null) return "";
        String trimmed = content.trim();
        if (trimmed.isEmpty()) return "";

        // 개행 제거(알림 문구 깨짐 방지)
        String singleLine = trimmed.replaceAll("\\s+", " ");

        if (singleLine.length() <= maxLength) {
            return singleLine;
        }
        return singleLine.substring(0, maxLength) + "...";
    }
}
