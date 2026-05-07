package com.shu.backend.domain.chatmessage.controller;

import com.shu.backend.domain.chatmessage.dto.ChatMessageDTO;
import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageSuccessStatus;
import com.shu.backend.domain.chatmessage.service.ChatMessageService;
import com.shu.backend.domain.chatroom.entity.ChatRoom;
import com.shu.backend.domain.chatroom.repository.ChatRoomRepository;
import com.shu.backend.domain.penalty.security.PenaltyChecker;
import com.shu.backend.global.apiPayload.ApiResponse;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import com.shu.backend.global.websocket.ChatRealtimePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatMessageWsController {

    private static final int MAX_TEXT_LENGTH = 500;

    private final ChatMessageService chatMessageService;
    private final ChatRealtimePublisher realtimePublisher;
    private final ChatRoomRepository chatRoomRepository;
    private final PenaltyChecker penaltyChecker;

    // =================== 실시간 메시지 전송 ===================
    // Client -> /pub/chat.send
    // Server -> /sub/chat/rooms/{roomId}
    @MessageMapping("/chat.send")
    public void send(ChatMessageDTO.SendRequest request, Principal principal) {

        if (principal == null || principal.getName() == null) {
            throw new ChatMessageException(ChatMessageErrorStatus.UNAUTHORIZED_WS);
        }

        Long senderId = Long.valueOf(principal.getName());

        if (!penaltyChecker.notPenalized(senderId)) {
            ErrorReasonDto reason = ChatMessageErrorStatus.CHAT_PENALIZED.getReason();
            realtimePublisher.publish(
                    "/sub/chat/rooms/" + request.getRoomId(),
                    ApiResponse.onFailure(
                            reason.getCode(),
                            reason.getMessage(),
                            ChatMessageDTO.SendErrorBroadcast.builder()
                                    .eventType("SEND_ERROR")
                                    .type("SEND_ERROR")
                                    .senderId(senderId)
                                    .code(reason.getCode())
                                    .message(reason.getMessage())
                                    .build()
                    )
            );
            return;
        }

        if (request.getType() == ChatMessageDTO.MessageType.TEXT) {
            if (request.getContent() == null || request.getContent().isBlank()) {
                throw new ChatMessageException(ChatMessageErrorStatus.INVALID_MESSAGE_TYPE);
            }
            if (request.getContent().length() > MAX_TEXT_LENGTH) {
                throw new ChatMessageException(ChatMessageErrorStatus.MESSAGE_TOO_LONG);
            }
        }

        ChatMessageDTO.MessageResponse result;
        try {
            result = chatMessageService.send(senderId, request);
        } catch (ChatMessageException e) {
            ErrorReasonDto reason = e.getErrorReason();

            // STOMP 전송은 HTTP 응답처럼 예외 본문이 클라이언트로 바로 돌아가지 않는다.
            // 전송자만 자신의 실패로 처리할 수 있도록 senderId가 포함된 SEND_ERROR 이벤트를 같은 방에 보낸다.
            realtimePublisher.publish(
                    "/sub/chat/rooms/" + request.getRoomId(),
                    ApiResponse.onFailure(
                            reason.getCode(),
                            reason.getMessage(),
                            ChatMessageDTO.SendErrorBroadcast.builder()
                                    .eventType("SEND_ERROR")
                                    .type("SEND_ERROR")
                                    .senderId(senderId)
                                    .code(reason.getCode())
                                    .message(reason.getMessage())
                                    .build()
                    )
            );
            return;
        }

        realtimePublisher.publish(
                "/sub/chat/rooms/" + request.getRoomId(),
                ApiResponse.of(ChatMessageSuccessStatus.CHAT_MESSAGE_SEND_SUCCESS,
                        ChatMessageDTO.MessageCreatedBroadcast.builder()
                                .eventType("MESSAGE_CREATED")
                                .message(result)
                                .build())
        );
        publishRoomUpdatedToParticipants(result.getRoomId());
    }

    private void publishRoomUpdatedToParticipants(Long roomId) {
        chatRoomRepository.findById(roomId).ifPresent(room -> {
            publishRoomUpdated(room.getUser1Id(), room);
            publishRoomUpdated(room.getUser2Id(), room);
        });
    }

    private void publishRoomUpdated(Long userId, ChatRoom room) {
        // 채팅방 목록은 payload로 직접 수정하지 않고, 이벤트 수신자가 목록 API를 재조회한다.
        // 이렇게 하면 미읽음 수/마지막 메시지/정렬 기준을 서버 계산값과 항상 맞출 수 있다.
        realtimePublisher.publish(
                "/sub/chat/users/" + userId + "/rooms",
                ApiResponse.of(ChatMessageSuccessStatus.CHAT_MESSAGE_SEND_SUCCESS,
                        ChatMessageDTO.RoomUpdatedBroadcast.builder()
                                .eventType("ROOM_LIST_UPDATED")
                                .type("ROOM_LIST_UPDATED")
                                .roomId(room.getId())
                                .build())
        );
    }
}
