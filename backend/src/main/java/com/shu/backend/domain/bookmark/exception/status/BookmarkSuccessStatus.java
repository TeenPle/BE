package com.shu.backend.domain.bookmark.exception.status;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookmarkSuccessStatus implements BaseCode {

    BOOKMARK_ADDED(HttpStatus.OK, "BOOKMARK200", "북마크에 추가되었습니다."),
    BOOKMARK_REMOVED(HttpStatus.OK, "BOOKMARK201", "북마크가 해제되었습니다."),
    BOOKMARK_LIST_FETCHED(HttpStatus.OK, "BOOKMARK202", "북마크 목록을 조회했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDto getReason() {
        return ReasonDto.builder()
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ReasonDto getReasonHttpStatus() {
        return ReasonDto.builder()
                .httpStatus(httpStatus)
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }
}
