package com.shu.backend.domain.usersetting.controller;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.usersetting.dto.UserSettingDTO;
import com.shu.backend.domain.usersetting.exception.status.UserSettingSuccessStatus;
import com.shu.backend.domain.usersetting.service.UserSettingService;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "UserSetting",
        description = "유저 알림/설정 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-settings")
public class UserSettingController {

    private final UserSettingService userSettingService;

    @Operation(
            summary = "내 유저 설정 조회",
            description = "현재 로그인한 사용자의 알림 설정을 조회합니다. 설정이 없으면 기본값으로 자동 생성됩니다."
    )
    @GetMapping("/me")
    public ApiResponse<UserSettingDTO.Response> getMySetting(@AuthenticationPrincipal User user) {
        return ApiResponse.onSuccess(userSettingService.getMySetting(user.getId()));
    }

    @Operation(
            summary = "내 유저 설정 수정",
            description = "현재 로그인한 사용자의 알림 설정을 부분 수정(PATCH)합니다."
    )
    @PatchMapping("/me")
    public ApiResponse<UserSettingDTO.Response> updateMySetting(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserSettingDTO.UpdateRequest req
    ) {
        return ApiResponse.onSuccess(userSettingService.updateMySetting(user.getId(), req));
    }
}
