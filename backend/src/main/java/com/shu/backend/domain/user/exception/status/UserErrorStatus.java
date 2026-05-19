package com.shu.backend.domain.user.exception.status;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.BaseErrorCode;
import com.shu.backend.global.apiPayload.code.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorStatus implements BaseErrorCode {

    // =================== 400 / 401 ===================
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4000", "존재하지 않는 사용자입니다."),
    EXIST_EMAIL(HttpStatus.NOT_FOUND, "USER4001", "이미 존재하는 이메일입니다."),
    EXIST_NICKNAME(HttpStatus.NOT_FOUND, "USER4002", "이미 존재하는 닉네임입니다."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4003", "존재하지 않는 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "USER4004", "비밀번호가 일치하지 않습니다."),
    SCHOOL_VERIFICATION_ALREADY_APPROVED(HttpStatus.BAD_REQUEST, "USER4005", "이미 학교 인증이 완료된 사용자입니다."),
    SAME_NICKNAME(HttpStatus.BAD_REQUEST, "USER4006", "현재와 동일한 닉네임입니다."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "USER4007", "현재와 동일한 비밀번호입니다."),
    NICKNAME_CHANGE_COOLDOWN(HttpStatus.BAD_REQUEST, "USER4008", "닉네임은 30일에 한 번만 변경할 수 있습니다."),
    INACTIVE_USER(HttpStatus.FORBIDDEN, "USER4009", "Inactive user."),
    /** 탈퇴 유예 기간 중 로그인 시도 — 복구 화면으로 분기하기 위한 전용 코드 */
    ACCOUNT_PENDING_DELETION(HttpStatus.FORBIDDEN, "USER4051", "탈퇴 대기 중인 계정입니다. 7일 이내에 복구할 수 있습니다."),

    VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "USER4010", "인증번호가 올바르지 않습니다."),
    VERIFICATION_TOKEN_INVALID_OR_EXPIRED(HttpStatus.BAD_REQUEST, "USER4011", "인증 토큰이 만료되었거나 유효하지 않습니다."),
    VERIFICATION_TARGET_MISMATCH(HttpStatus.BAD_REQUEST, "USER4012", "휴대폰 번호가 일치하지 않습니다."),
    VERIFICATION_CODE_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "USER4013", "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요."),

    // =================== 403 ===================
    SCHOOL_VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "USER4031", "학교 인증이 필요합니다. 학생증을 업로드해주세요."),
    SCHOOL_VERIFICATION_PENDING(HttpStatus.FORBIDDEN, "USER4032", "학교 인증 심사 중입니다. 인증 완료 후 로그인 가능합니다."),
    SCHOOL_VERIFICATION_REJECTED(HttpStatus.FORBIDDEN, "USER4033", "학교 인증이 거절되었습니다."),

    // =================== Refresh Token ===================
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4120", "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH4121", "Refresh Token이 만료되었습니다. 다시 로그인해주세요."),

    // =================== 500 ===================
    USER_STUDENT_CARD_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "USER5000", "학생증 이미지 업로드에 실패했습니다."),
    SCHOOL_VERIFICATION_STATUS_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "USER5001", "잘못된 인증 상태입니다. 관리자에게 문의해주세요."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "USER4040", "허용되지 않는 파일 형식입니다. (허용: jpg, png)"),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "USER4041", "파일 내용이 확장자와 일치하지 않습니다.");
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
