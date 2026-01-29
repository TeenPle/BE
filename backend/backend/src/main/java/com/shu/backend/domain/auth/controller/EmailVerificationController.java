package com.shu.backend.domain.auth.controller;

import com.shu.backend.domain.auth.service.VerificationService;
import com.shu.backend.domain.user.exception.status.UserSuccessStatus;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth - Email Verification", description = "이메일 인증(OTP) 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/email")
public class EmailVerificationController {

    private final VerificationService verificationService;

    /**
     * 해당 API를 호출할 경우 입력된 이메일로 인증 코드 전송.
     * @param request = 이메일 필드를 가짐.
     * @return 이메일 전송 성공 메시지
     */
    @PostMapping(value = "/send", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Void> send(@RequestBody @Valid SendRequest request) {
        verificationService.sendCode(request.email());
        return ApiResponse.of(UserSuccessStatus.EMAIL_VERIFICATION_CODE_SENT, null);
    }

    /**
     * 해당 API를 호출할 경우 인증코드가 맞는지 확인.
     * @param request = 이메일과 코드를 가지고 있는 DTO
     * @return 인증 성공 메시지
     */
    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<VerifyResponse> verify(@RequestBody @Valid VerifyRequest request) {

        String verificationToken =
                verificationService.verifyCode(request.email(), request.code());

        return ApiResponse.of(
                UserSuccessStatus.EMAIL_VERIFICATION_SUCCESS,
                new VerifyResponse(verificationToken)
        );
    }

    @Schema(description = "이메일 인증코드 발송 요청 DTO")
    public record SendRequest(
            @Schema(description = "인증코드를 받을 이메일", example = "user@naver.com")
            @NotBlank
            @jakarta.validation.constraints.Email(message = "이메일 형식이 올바르지 않습니다.")
            String email
    ) {}

    @Schema(description = "이메일 인증코드 검증 요청 DTO")
    public record VerifyRequest(
            @Schema(description = "이메일", example = "user@naver.com")
            @NotBlank
            @jakarta.validation.constraints.Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @Schema(description = "6자리 이메일 인증코드", example = "123456")
            @NotBlank
            @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자여야 합니다.")
            String code
    ) {}

    public record VerifyResponse(String verificationToken) {}
}