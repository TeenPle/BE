package com.shu.backend.domain.poll.exception.status;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PollErrorStatus implements BaseErrorCode {

    INVALID_POLL_OPTIONS(HttpStatus.BAD_REQUEST, "POLL4000", "투표 항목은 2개 이상 5개 이하로 입력해야 합니다."),
    DUPLICATE_POLL_OPTIONS(HttpStatus.BAD_REQUEST, "POLL4001", "투표 항목은 중복될 수 없습니다."),
    POLL_OPTION_TOO_LONG(HttpStatus.BAD_REQUEST, "POLL4002", "투표 항목은 100자 이하로 입력해야 합니다."),
    POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "POLL4003", "투표가 존재하지 않습니다."),
    POLL_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "POLL4004", "투표 항목이 존재하지 않습니다."),
    POLL_ALREADY_VOTED(HttpStatus.BAD_REQUEST, "POLL4005", "이미 투표에 참여했습니다."),
    POLL_OPTION_MISMATCH(HttpStatus.BAD_REQUEST, "POLL4006", "해당 게시글의 투표 항목이 아닙니다.");

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
