package com.shu.backend.domain.chatroom.service;


import com.shu.backend.domain.chatmessage.entity.ChatMessage;
import com.shu.backend.domain.chatmessage.repository.ChatMessageRepository;
import com.shu.backend.domain.chatroom.dto.ChatRoomDTO;
import com.shu.backend.domain.chatroom.entity.ChatRoom;
import com.shu.backend.domain.chatroom.exception.ChatRoomException;
import com.shu.backend.domain.chatroom.exception.status.ChatRoomErrorStatus;
import com.shu.backend.domain.chatroom.repository.ChatRoomRepository;
import com.shu.backend.domain.chatroomuser.entity.ChatRoomUser;
import com.shu.backend.domain.chatroomuser.repository.ChatRoomUserRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 게시글 기반 1:1 채팅방 조회 또는 생성
    public ChatRoomDTO.CreateDmResponse findOrCreateDm(Long myId, Long otherId, Long sourcePostId, String roomTitle) {

        long u1 = Math.min(myId, otherId);
        long u2 = Math.max(myId, otherId);

        ChatRoom room = chatRoomRepository.findByUser1IdAndUser2IdAndSourcePostId(u1, u2, sourcePostId)
                .orElseGet(() -> {
                    String title = (roomTitle != null && !roomTitle.isBlank()) ? roomTitle : "채팅방";
                    ChatRoom created = chatRoomRepository.save(ChatRoom.ofDm(u1, u2, sourcePostId, title));

                    User me = userRepository.findById(myId)
                            .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
                    User other = userRepository.findById(otherId)
                            .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

                    chatRoomUserRepository.save(ChatRoomUser.createHidden(created, me));
                    chatRoomUserRepository.save(ChatRoomUser.createHidden(created, other));

                    return created;
                });

        ChatRoomUser myCru = chatRoomUserRepository.findByChatRoomIdAndUserId(room.getId(), myId)
                .orElse(null);
        ChatRoomUser otherCru = chatRoomUserRepository.findByChatRoomIdAndUserId(room.getId(), otherId)
                .orElse(null);
        boolean blockedByMe = myCru != null && myCru.isBlocked();
        boolean blockedByOther = otherCru != null && otherCru.isBlocked();

        return ChatRoomDTO.CreateDmResponse.builder()
                .roomId(room.getId())
                .otherUserId(otherId)
                .lastMessageAt(room.getLastMessageAt())
                .displayName(room.getDisplayName())
                .blocked(blockedByMe || blockedByOther)
                .blockedByMe(blockedByMe)
                .blockedByOther(blockedByOther)
                .build();
    }

    // 내 채팅방 목록 조회
    @Transactional(readOnly = true)
    public ChatRoomDTO.RoomListResponse getMyRooms(Long myId) {

        List<ChatRoomUser> mine =
                new java.util.ArrayList<>(chatRoomUserRepository.findByUserIdAndHiddenFalse(myId));

        mine.sort(Comparator.comparing(
                cru -> cru.getChatRoom().getLastMessageAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // 마지막 메시지 일괄 조회 (N+1 방지)
        List<Long> lastMsgIds = mine.stream()
                .map(cru -> cru.getChatRoom().getLastMessageId())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, ChatMessage> lastMsgMap = chatMessageRepository.findAllById(lastMsgIds)
                .stream()
                .collect(Collectors.toMap(ChatMessage::getId, m -> m));

        List<Long> roomIds = mine.stream()
                .map(cru -> cru.getChatRoom().getId())
                .toList();

        Map<String, ChatRoomUser> chatRoomUserMap = roomIds.isEmpty()
                ? Map.of()
                : chatRoomUserRepository.findByChatRoomIdIn(roomIds)
                .stream()
                .collect(Collectors.toMap(
                        cru -> cru.getChatRoom().getId() + ":" + cru.getUser().getId(),
                        cru -> cru
                ));

        Map<Long, Long> unreadMap = roomIds.isEmpty()
                ? Map.of()
                : chatMessageRepository.countUnreadByRoomIds(roomIds, myId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        List<ChatRoomDTO.RoomListItem> items = mine.stream().map(cru -> {
            ChatRoom room = cru.getChatRoom();
            Long otherId = room.getUser1Id().equals(myId) ? room.getUser2Id() : room.getUser1Id();
            boolean blockedByMe = cru.isBlocked();
            ChatRoomUser otherCru = chatRoomUserMap.get(room.getId() + ":" + otherId);
            boolean blockedByOther = otherCru != null && otherCru.isBlocked();

            // 실제 마지막 메시지 내용으로 미리보기
            String preview = "";
            if (room.getLastMessageId() != null) {
                ChatMessage lastMsg = lastMsgMap.get(room.getLastMessageId());
                if (lastMsg != null) {
                    preview = lastMsg.getType() == ChatMessage.MessageType.IMAGE
                            ? "[사진]"
                            : (lastMsg.getContent() != null ? lastMsg.getContent() : "");
                }
            }

            long unread = unreadMap.getOrDefault(room.getId(), 0L);

            return ChatRoomDTO.RoomListItem.builder()
                    .roomId(room.getId())
                    .otherUserId(otherId)
                    .lastPreview(preview)
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(unread)
                    .displayName(room.getDisplayName())
                    .blocked(blockedByMe || blockedByOther)
                    .blockedByMe(blockedByMe)
                    .blockedByOther(blockedByOther)
                    .build();
        }).toList();

        return ChatRoomDTO.RoomListResponse.builder()
                .rooms(items)
                .build();
    }

    // 채팅방 나가기
    public void leave(Long myId, Long roomId) {
        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));
        cru.leave();
    }

    // 채팅방 차단
    public void block(Long myId, Long roomId) {
        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));
        cru.block();
    }

    // 채팅방 차단 해제
    public void unblock(Long myId, Long roomId) {
        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));
        cru.unblock();
    }

    // 채팅방 신고
    public void report(Long myId, Long roomId, String reason) {
        chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));
        log.info("채팅방 신고 - reporterId={}, roomId={}, reason={}", myId, roomId, reason);
    }

    @Transactional(readOnly = true)
    public List<Long> getParticipantIds(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.CHAT_ROOM_NOT_FOUND));
        return List.of(room.getUser1Id(), room.getUser2Id());
    }
}
