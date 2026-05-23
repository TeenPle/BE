package com.shu.backend.global.websocket;

import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.chatroomuser.repository.ChatRoomUserRepository;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WsJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;

    private static final Pattern ROOM_SUB_PATTERN =
            Pattern.compile("^/sub/chat/rooms/(\\d+)$");
    private static final Pattern USER_ROOM_SUB_PATTERN =
            Pattern.compile("^/sub/chat/users/(\\d+)/rooms$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // wrap()은 헤더 복사본을 만들어 setUser()가 원본 message에 반영되지 않음
        // getAccessor()로 message에 연결된 mutable accessor를 직접 가져와야 세션에 user가 저장됨
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        /* ================= CONNECT ================= */
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String auth = accessor.getFirstNativeHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new ChatMessageException(ChatMessageErrorStatus.UNAUTHORIZED_WS);
            }

            String token = auth.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                throw new ChatMessageException(ChatMessageErrorStatus.INVALID_WS_TOKEN);
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            // 탈퇴·비활성 유저는 WebSocket 연결 자체를 차단한다.
            if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
                throw new ChatMessageException(ChatMessageErrorStatus.UNAUTHORIZED_WS);
            }

            accessor.setUser(new WsPrincipal(userId.toString()));
            return message;
        }

        /* ================= 인증 공통 ================= */
        Principal principal = accessor.getUser();
        if (principal == null || principal.getName() == null) {
            throw new ChatMessageException(ChatMessageErrorStatus.UNAUTHORIZED_WS);
        }

        /* ================= SUBSCRIBE ================= */
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            String destination = accessor.getDestination();
            if (destination == null) return message;

            Matcher matcher = ROOM_SUB_PATTERN.matcher(destination);
            if (matcher.matches()) {
                Long roomId = Long.valueOf(matcher.group(1));
                Long userId = Long.valueOf(principal.getName());

                boolean isMember = chatRoomUserRepository
                        .findByChatRoomIdAndUserId(roomId, userId)
                        .isPresent();

                if (!isMember) {
                    throw new ChatMessageException(ChatMessageErrorStatus.WS_NOT_ROOM_MEMBER);
                }
            }

            Matcher userRoomMatcher = USER_ROOM_SUB_PATTERN.matcher(destination);
            if (userRoomMatcher.matches()) {
                Long destinationUserId = Long.valueOf(userRoomMatcher.group(1));
                Long userId = Long.valueOf(principal.getName());

                // 유저별 채팅 목록 이벤트는 본인 채널만 구독 가능해야 다른 사용자의 방 갱신 여부가 노출되지 않는다.
                if (!destinationUserId.equals(userId)) {
                    throw new ChatMessageException(ChatMessageErrorStatus.WS_NOT_ROOM_MEMBER);
                }
            }
        }

        return message;
    }

    private record WsPrincipal(String name) implements Principal {
        @Override public String getName() { return name; }
    }
}
