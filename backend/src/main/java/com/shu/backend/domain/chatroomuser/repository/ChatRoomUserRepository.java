package com.shu.backend.domain.chatroomuser.repository;

import com.shu.backend.domain.chatroomuser.entity.ChatRoomUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {
    Optional<ChatRoomUser> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    // 내 방 목록 (숨김X). 차단한 방도 목록에 남겨 입력 비활성화/차단 해제를 제공한다.
    @EntityGraph(attributePaths = "chatRoom")
    List<ChatRoomUser> findByUserIdAndHiddenFalse(Long userId);

    @EntityGraph(attributePaths = {"chatRoom", "user"})
    List<ChatRoomUser> findByChatRoomIdIn(List<Long> chatRoomIds);
}
