package com.shu.backend.domain.post.exception.status;



import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PostErrorStatus implements BaseErrorCode {
    //400
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST4000", "존재하지 않는 게시글입니다."),
    NO_PERMISSION_TO_WRITE(HttpStatus.BAD_REQUEST, "POST4001", "글 작성 권한이 없습니다."),
    POST_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "POST4002", "이미 삭제된 게시글입니다."),
    SEARCH_KEYWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "POST4003", "검색어는 100자 이하로 입력해주세요."),
    INAPPROPRIATE_CONTENT(HttpStatus.BAD_REQUEST, "POST4004", "게시글에 부적절한 내용이 포함되어 있습니다."),
    SELF_HARM_CONTENT(HttpStatus.BAD_REQUEST, "POST4005", "자해·자살 유도 내용이 포함되어 있습니다. 힘드시다면 자살예방상담전화(☎1393)로 연락해 주세요."),
    SEXUAL_CONTENT(HttpStatus.BAD_REQUEST, "POST4006", "성매매·불법 성적 거래 관련 내용이 포함되어 있습니다."),
    DRUG_CONTENT(HttpStatus.BAD_REQUEST, "POST4007", "불법 약물 거래 관련 내용이 포함되어 있습니다."),
    CONTACT_SOLICITATION(HttpStatus.BAD_REQUEST, "POST4008", "외부 개인연락 유도 내용이 포함되어 있습니다. (전화번호, 메신저 ID 등)"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "POST4009", "허용되지 않는 파일 형식입니다. (허용: jpg, png, gif, webp, mp4, mov)");



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
