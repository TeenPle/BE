package com.shu.backend.domain.media.exception.status;

import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MediaErrorStatus implements BaseErrorCode {

    POST_MEDIA_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA5000", "미디어 업로드에 실패했습니다."),
    POST_MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND, "MEDIA4040", "미디어를 찾을 수 없습니다."),
    POST_MEDIA_NO_PERMISSION(HttpStatus.FORBIDDEN, "MEDIA4030", "미디어에 접근 권한이 없습니다."),
    INAPPROPRIATE_IMAGE(HttpStatus.BAD_REQUEST, "MEDIA4001", "부적절한 사진이 감지되어 업로드할 수 없습니다. 다른 사진으로 다시 시도해 주세요."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "MEDIA4002", "허용되지 않는 파일 형식입니다. (허용: jpg, png, gif, webp, mp4, mov)"),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "MEDIA4003", "파일 내용이 확장자와 일치하지 않습니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "MEDIA4004", "파일 크기가 너무 큽니다. 이미지는 10MB, 동영상은 100MB 이하로 업로드해 주세요.");

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
