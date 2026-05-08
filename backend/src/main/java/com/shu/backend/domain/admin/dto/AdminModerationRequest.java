package com.shu.backend.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminModerationRequest {
    @NotBlank(message = "처리 사유를 입력해주세요.")
    @Size(max = 500, message = "처리 사유는 500자 이하로 입력해주세요.")
    private String reason;
}
