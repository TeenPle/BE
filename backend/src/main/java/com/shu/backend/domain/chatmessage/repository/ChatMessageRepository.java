package com.shu.backend.domain.chatmessage.repository;

import com.shu.backend.domain.chatmessage.entity.ChatMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findTop50ByChatRoomIdOrderByIdDesc(Long roomId);

    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findTop50ByChatRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long lastId);

    boolean existsByIdAndChatRoomId(Long id, Long chatRoomId);

    // 미읽음 개수 계산: lastReadId 이후 메시지 수
    long countByChatRoomIdAndIdGreaterThanAndSenderIdNot(Long chatRoomId, Long id, Long senderId);

    // 한 번도 읽지 않은 경우: 룸 전체 상대방 메시지 수
    long countByChatRoomIdAndSenderIdNot(Long chatRoomId, Long senderId);

    @Query("""
            select m.chatRoom.id, count(m)
            from ChatMessage m, ChatRoomUser cru
            where m.chatRoom.id in :roomIds
              and cru.chatRoom.id = m.chatRoom.id
              and cru.user.id = :userId
              and m.sender.id <> :userId
              and (cru.lastReadMessageId is null or m.id > cru.lastReadMessageId)
            group by m.chatRoom.id
            """)
    List<Object[]> countUnreadByRoomIds(@Param("roomIds") List<Long> roomIds,
                                        @Param("userId") Long userId);
}
