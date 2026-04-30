package com.shu.backend.domain.warning.exception.status;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WarningErrorStatus implements BaseErrorCode {

    WARNING_NOT_FOUND(HttpStatus.NOT_FOUND, "WARNING4040", "경고 정보를 찾을 수 없습니다."),
    WARNING_ALREADY_ISSUED(HttpStatus.CONFLICT, "WARNING4090", "이미 경고가 발령된 신고입니다."),
    WARNING_ACCESS_DENIED(HttpStatus.FORBIDDEN, "WARNING4030", "해당 경고에 접근할 권한이 없습니다.");

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
