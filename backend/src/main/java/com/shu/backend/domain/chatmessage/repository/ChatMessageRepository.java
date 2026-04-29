package com.shu.backend.domain.chatmessage.repository;

import com.shu.backend.domain.chatmessage.entity.ChatMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findTop50ByChatRoomIdOrderByIdDesc(Long roomId);

    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findTop50ByChatRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long lastId);

    // 미읽음 개수 계산: lastReadId 이후 메시지 수
    long countByChatRoomIdAndIdGreaterThanAndSenderIdNot(Long chatRoomId, Long id, Long senderId);

    // 한 번도 읽지 않은 경우: 룸 전체 상대방 메시지 수
    long countByChatRoomIdAndSenderIdNot(Long chatRoomId, Long senderId);
}
