package com.shu.backend.domain.chatroom.service;


import com.shu.backend.domain.chatmessage.repository.ChatMessageRepository;
import com.shu.backend.domain.chatroom.dto.ChatRoomDTO;
import com.shu.backend.domain.chatroom.entity.ChatRoom;
import com.shu.backend.domain.chatroom.exception.ChatRoomException;
import com.shu.backend.domain.chatroom.exception.status.ChatRoomErrorStatus;
import com.shu.backend.domain.chatroom.repository.ChatRoomRepository;
import com.shu.backend.domain.chatroomuser.entity.ChatRoomUser;
import com.shu.backend.domain.chatroomuser.repository.ChatRoomUserRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;


/**
 * 1:1 채팅방 생성, 목록 조회, 나가기, 차단 등
 * 채팅방과 참여자 상태 관련 비즈니스 로직을 처리하는 서비스.
 *
 * DM 채팅방을 조회하거나 없으면 생성하고,
 * 사용자 기준 채팅방 목록과 참여 상태를 관리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 1:1 채팅방 조회 또는 생성
    public ChatRoomDTO.CreateDmResponse findOrCreateDm(Long myId, Long otherId) {

        long u1 = Math.min(myId, otherId);
        long u2 = Math.max(myId, otherId);

        ChatRoom room = chatRoomRepository.findByUser1IdAndUser2Id(u1, u2)
                .orElseGet(() -> {
                    // 정규화된 두 사용자 기준으로 DM 방 생성
                    ChatRoom created = chatRoomRepository.save(ChatRoom.ofDm(u1, u2));

                    User me = userRepository.getReferenceById(myId);
                    User other = userRepository.getReferenceById(otherId);

                    // 최초 생성 시 양쪽 사용자 모두 숨김 상태로 참여 정보 생성
                    chatRoomUserRepository.save(ChatRoomUser.createHidden(created, me));
                    chatRoomUserRepository.save(ChatRoomUser.createHidden(created, other));

                    return created;
                });

        return ChatRoomDTO.CreateDmResponse.builder()
                .roomId(room.getId())
                .otherUserId(otherId)
                .lastMessageAt(room.getLastMessageAt())
                .displayName(room.getDisplayName())
                .build();
    }

    // 내 채팅방 목록 조회
    @Transactional(readOnly = true)
    public ChatRoomDTO.RoomListResponse getMyRooms(Long myId) {

        List<ChatRoomUser> mine =
                new java.util.ArrayList<>(chatRoomUserRepository.findByUserIdAndHiddenFalseAndBlockedAtIsNull(myId));

        // 마지막 메시지 시각 기준 최신순 정렬
        mine.sort(Comparator.comparing(
                cru -> cru.getChatRoom().getLastMessageAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        // 채팅방 목록 응답 DTO 변환
        List<ChatRoomDTO.RoomListItem> items = mine.stream().map(cru -> {
            ChatRoom room = cru.getChatRoom();

            // 1:1 채팅 기준 상대방 ID 계산
            Long otherId = room.getUser1Id().equals(myId) ? room.getUser2Id() : room.getUser1Id();

            // 마지막 메시지 미리보기(MVP 단계 문자열 처리)
            String preview = (room.getLastMessageId() == null) ? "" : "(마지막 메시지)";

            // 미읽음 여부 계산(MVP 단계 0/1 처리)
            long unread = 0L;
            if (room.getLastMessageId() != null) {
                Long lastRead = cru.getLastReadMessageId();
                unread = (lastRead == null) ? 1L : (room.getLastMessageId() > lastRead ? 1L : 0L);
            }

            return ChatRoomDTO.RoomListItem.builder()
                    .roomId(room.getId())
                    .otherUserId(otherId)
                    .lastPreview(preview)
                    .lastMessageAt(room.getLastMessageAt())
                    .unreadCount(unread)
                    .displayName(room.getDisplayName())
                    .build();
        }).toList();

        return ChatRoomDTO.RoomListResponse.builder()
                .rooms(items)
                .build();
    }

    // 채팅방 나가기 처리
    public void leave(Long myId, Long roomId) {

        // 참여자 정보 조회
        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));

        cru.leave();
    }

    // 채팅방 차단 처리
    public void block(Long myId, Long roomId) {

        // 참여자 정보 조회
        ChatRoomUser cru = chatRoomUserRepository.findByChatRoomIdAndUserId(roomId, myId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorStatus.NOT_ROOM_MEMBER));

        cru.block();
    }
}