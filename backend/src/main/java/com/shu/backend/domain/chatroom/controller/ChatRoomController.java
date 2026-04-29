package com.shu.backend.domain.chatroom.controller;

import com.shu.backend.domain.chatmessage.dto.ChatMessageDTO;
import com.shu.backend.domain.chatroom.dto.ChatRoomDTO;
import com.shu.backend.domain.chatroom.exception.status.ChatRoomSuccessStatus;
import com.shu.backend.domain.chatroom.service.ChatRoomService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @Operation(
            summary = "1:1 DM 채팅방 생성/조회",
            description = "게시글 기준으로 1:1 DM 채팅방을 조회하고, 없으면 생성합니다. " +
                          "같은 두 사용자라도 다른 게시글에서 유입되면 별도의 채팅방이 생성됩니다."
    )
    @PostMapping("/dm")
    public ApiResponse<ChatRoomDTO.CreateDmResponse> createOrGetDm(
            @Parameter(description = "로그인 사용자", required = true)
            @AuthenticationPrincipal User user,

            @RequestBody @Valid ChatRoomDTO.CreateDmRequest request
    ) {
        ChatRoomDTO.CreateDmResponse result = chatRoomService.findOrCreateDm(
                user.getId(),
                request.getOtherUserId(),
                request.getSourcePostId(),
                request.getRoomTitle()
        );
        return ApiResponse.of(ChatRoomSuccessStatus.DM_ROOM_FIND_OR_CREATE_SUCCESS, result);
    }

    @Operation(
            summary = "내 채팅방 목록 조회",
            description = "숨김 제외한 내 채팅방 목록을 최신 메시지 기준으로 조회합니다. 차단된 방도 목록에 유지합니다."
    )
    @GetMapping
    public ApiResponse<ChatRoomDTO.RoomListResponse> myRooms(
            @Parameter(description = "로그인 사용자", required = true)
            @AuthenticationPrincipal User user
    ) {
        ChatRoomDTO.RoomListResponse result = chatRoomService.getMyRooms(user.getId());
        return ApiResponse.of(ChatRoomSuccessStatus.CHAT_ROOM_LIST_SUCCESS, result);
    }

    @Operation(
            summary = "채팅방 나가기",
            description = "채팅방을 나갑니다."
    )
    @PostMapping("/{roomId}/leave")
    public ApiResponse<String> leave(
            @AuthenticationPrincipal User user,
            @PathVariable Long roomId
    ) {
        chatRoomService.leave(user.getId(), roomId);
        publishRoomUpdated(user.getId(), roomId);
        return ApiResponse.of(ChatRoomSuccessStatus.CHAT_ROOM_LEAVE_SUCCESS, "OK");
    }

    @Operation(
            summary = "채팅방 차단",
            description = "채팅방을 차단합니다. 차단 상태에서는 채팅이 불가능합니다."
    )
    @PostMapping("/{roomId}/block")
    public ApiResponse<String> block(
            @AuthenticationPrincipal User user,
            @PathVariable Long roomId
    ) {
        chatRoomService.block(user.getId(), roomId);
        publishRoomUpdatedToParticipants(roomId);
        return ApiResponse.of(ChatRoomSuccessStatus.CHAT_ROOM_BLOCK_SUCCESS, "OK");
    }

    @Operation(
            summary = "채팅방 차단 해제",
            description = "차단한 채팅방을 다시 채팅 가능한 상태로 변경합니다."
    )
    @PostMapping("/{roomId}/unblock")
    public ApiResponse<String> unblock(
            @AuthenticationPrincipal User user,
            @PathVariable Long roomId
    ) {
        chatRoomService.unblock(user.getId(), roomId);
        publishRoomUpdatedToParticipants(roomId);
        return ApiResponse.of(ChatRoomSuccessStatus.CHAT_ROOM_UNBLOCK_SUCCESS, "OK");
    }

    @Operation(
            summary = "채팅방 신고",
            description = "채팅방(상대방)을 신고합니다."
    )
    @PostMapping("/{roomId}/report")
    public ApiResponse<String> report(
            @AuthenticationPrincipal User user,
            @PathVariable Long roomId,
            @RequestBody ChatRoomDTO.ReportRequest request
    ) {
        chatRoomService.report(user.getId(), roomId, request.getReason());
        return ApiResponse.of(ChatRoomSuccessStatus.CHAT_ROOM_REPORT_SUCCESS, "OK");
    }

    private void publishRoomUpdated(Long userId, Long roomId) {
        // 목록과 채팅방 상세가 REST로 최신 상태를 다시 가져오도록 가벼운 변경 신호만 보낸다.
        messagingTemplate.convertAndSend(
                "/sub/chat/users/" + userId + "/rooms",
                ApiResponse.of(ChatRoomSuccessStatus.CHAT_ROOM_LIST_SUCCESS,
                        ChatMessageDTO.RoomUpdatedBroadcast.builder()
                                .type("ROOM_UPDATED")
                                .roomId(roomId)
                                .build())
        );
    }

    private void publishRoomUpdatedToParticipants(Long roomId) {
        // 차단/차단 해제는 양쪽 입력 가능 상태가 바뀌므로 두 사용자에게 즉시 알린다.
        chatRoomService.getParticipantIds(roomId)
                .forEach(userId -> publishRoomUpdated(userId, roomId));
    }
}
