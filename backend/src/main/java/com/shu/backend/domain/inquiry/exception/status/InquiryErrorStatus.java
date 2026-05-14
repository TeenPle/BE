package com.shu.backend.domain.inquiry.exception.status;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum InquiryErrorStatus implements BaseErrorCode {

    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY4001", "문의를 찾을 수 없습니다."),
    INQUIRY_ALREADY_ANSWERED(HttpStatus.CONFLICT, "INQUIRY4002", "이미 답변된 문의입니다."),
    INQUIRY_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "INQUIRY4003", "문의 제목을 입력해주세요."),
    INQUIRY_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "INQUIRY4004", "문의 내용을 입력해주세요."),
    INQUIRY_ANSWER_REQUIRED(HttpStatus.BAD_REQUEST, "INQUIRY4005", "답변 내용을 입력해주세요.");

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
