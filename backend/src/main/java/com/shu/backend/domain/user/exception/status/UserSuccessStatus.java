package com.shu.backend.domain.user.exception.status;

import com.shu.backend.global.apiPayload.code.BaseCode;
import com.shu.backend.global.apiPayload.code.ReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserSuccessStatus implements BaseCode {
    // Auth
    USER_SIGNUP_SUCCESS(HttpStatus.CREATED, "USER2010", "회원가입 완료"),
    USER_LOGIN_SUCCESS(HttpStatus.OK, "USER2001", "로그인 성공"),
    USER_LOGOUT_SUCCESS(HttpStatus.OK, "USER2003", "로그아웃에 성공했습니다."),
    USER_TOKEN_REFRESH_SUCCESS(HttpStatus.OK, "USER2004", "토큰 갱신 성공"),
    FIND_EMAIL_SUCCESS(HttpStatus.OK, "USER2005", "이메일 찾기 성공"),
    PASSWORD_RESET_CODE_SENT(HttpStatus.OK, "USER2006", "비밀번호 재설정 인증번호 발송 성공"),
    PASSWORD_RESET_SUCCESS(HttpStatus.OK, "USER2007", "비밀번호 재설정 성공"),

    //  Email Verification
    EMAIL_VERIFICATION_CODE_SENT(HttpStatus.OK, "USER2011", "이메일 인증번호 발송 성공"),
    EMAIL_VERIFICATION_SUCCESS(HttpStatus.OK, "USER2012", "이메일 인증 성공"),

    // User
    USER_FOUND(HttpStatus.OK, "USER2002", "사용자 조회 성공"),

    // Profile
    USER_PROFILE_SUCCESS(HttpStatus.OK, "USER2020", "프로필 조회 성공"),
    USER_NICKNAME_UPDATE_SUCCESS(HttpStatus.OK, "USER2021", "닉네임 변경 성공"),
    USER_PASSWORD_UPDATE_SUCCESS(HttpStatus.OK, "USER2022", "비밀번호 변경 성공"),
    USER_MY_POSTS_SUCCESS(HttpStatus.OK, "USER2023", "내 게시글 조회 성공"),
    USER_MY_COMMENTS_SUCCESS(HttpStatus.OK, "USER2024", "내 댓글 조회 성공"),
    USER_DELETE_SUCCESS(HttpStatus.OK, "USER2025", "회원 탈퇴 요청 성공 (7일 후 완전 삭제)"),
    USER_RESTORE_SUCCESS(HttpStatus.OK, "USER2026", "계정 복구 성공"),
    USER_BOARD_PROFILES_SUCCESS(HttpStatus.OK, "USER2027", "게시판별 프로필 조회 성공"),
    USER_BOARD_PROFILE_UPDATE_SUCCESS(HttpStatus.OK, "USER2028", "게시판별 프로필 변경 성공"),

    // Verification
    VERIFICATION_APPROVED(HttpStatus.OK, "VERI2001", "학교 인증 승인 완료");

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
