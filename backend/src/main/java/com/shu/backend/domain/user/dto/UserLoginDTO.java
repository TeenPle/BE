package com.shu.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserLoginDTO {
    @NotBlank(message = "이메일은 필수 입력입니다.")
    @Schema(description = "사용자 이메일 ", example = "teenple@example.com")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력입니다.")
    @Schema(description = "비밀번호 (영문+숫자+특수문자 포함 8~20자)", example = "Abcd1234!")
    private String password;
}
