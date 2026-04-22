package com.shu.backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenRefreshResponseDTO {
    private String accessToken;
    private String refreshToken;
}
