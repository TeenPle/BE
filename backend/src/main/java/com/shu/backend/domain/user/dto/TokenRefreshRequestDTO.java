package com.shu.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenRefreshRequestDTO {

    @NotBlank(message = "Refresh token은 필수입니다.")
    private String refreshToken;
}
