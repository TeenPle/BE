package com.shu.backend.domain.penalty.exception.status;



import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PenaltyErrorStatus implements BaseErrorCode {
    //400
    PENALTY_NOT_FOUND(HttpStatus.NOT_FOUND, "PENALTY4000", "제재 정보를 찾을 수 없습니다."),
    PENALTY_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "PENALTY4001", "활성 상태의 제재만 취소할 수 있습니다.");



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
