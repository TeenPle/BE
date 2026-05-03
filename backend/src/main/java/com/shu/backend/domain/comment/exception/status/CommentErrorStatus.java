package com.shu.backend.domain.comment.exception.status;



import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommentErrorStatus implements BaseErrorCode {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT4000", "존재하지 않는 댓글입니다."),
    NO_PERMISSION_TO_WRITE(HttpStatus.BAD_REQUEST, "COMMENT4001", "댓글 작성 권한이 없습니다."),
    COMMENT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "COMMENT4002", "이미 삭제된 댓글입니다."),
    PARENT_COMMENT_NOT_IN_SAME_POST(HttpStatus.BAD_REQUEST, "COMMENT4003", "댓글과 대댓글의 게시글이 다릅니다."),
    INVALID_CONTENT(HttpStatus.BAD_REQUEST, "COMMENT4004", "댓글 내용이 잘못되었습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT4005", "댓글 수정 권한이 없습니다."),
    INAPPROPRIATE_CONTENT(HttpStatus.BAD_REQUEST, "COMMENT4006", "댓글에 부적절한 내용이 포함되어 있습니다."),
    SELF_HARM_CONTENT(HttpStatus.BAD_REQUEST, "COMMENT4007", "자해·자살 유도 내용이 포함되어 있습니다. 힘드시다면 자살예방상담전화(☎1393)로 연락해 주세요."),
    SEXUAL_CONTENT(HttpStatus.BAD_REQUEST, "COMMENT4008", "성매매·불법 성적 거래 관련 내용이 포함되어 있습니다."),
    DRUG_CONTENT(HttpStatus.BAD_REQUEST, "COMMENT4009", "불법 약물 거래 관련 내용이 포함되어 있습니다."),
    CONTACT_SOLICITATION(HttpStatus.BAD_REQUEST, "COMMENT4010", "외부 개인연락 유도 내용이 포함되어 있습니다. (전화번호, 메신저 ID 등)");



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
