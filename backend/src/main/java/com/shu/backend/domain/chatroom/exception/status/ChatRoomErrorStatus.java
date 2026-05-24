package com.shu.backend.domain.chatroom.exception.status;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatRoomErrorStatus implements BaseErrorCode {
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHATROOM4001", "채팅방을 찾을 수 없습니다."),
    NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "CHATROOM4002", "채팅방 참여자가 아닙니다."),
    TARGET_USER_DELETED(HttpStatus.FORBIDDEN, "CHATROOM4003", "탈퇴한 사용자에게는 이 작업을 할 수 없습니다."),
    SCHOOL_MISMATCH(HttpStatus.FORBIDDEN, "CHATROOM4004", "같은 학교 학생끼리만 채팅할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder()
                .httpStatus(httpStatus)
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }
}
