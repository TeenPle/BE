package com.shu.backend.domain.block.controller;

import com.shu.backend.domain.block.service.UserBlockService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
public class UserBlockController {

    private final UserBlockService userBlockService;

    @PostMapping("/{userId}")
    public ApiResponse<Void> blockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal User me
    ) {
        userBlockService.blockUser(me.getId(), userId);
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> unblockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal User me
    ) {
        userBlockService.unblockUser(me.getId(), userId);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getBlockSummary(
            @AuthenticationPrincipal User me
    ) {
        return ApiResponse.onSuccess(Map.of(
                "blockedCount", userBlockService.getBlockedCount(me.getId())
        ));
    }

    @DeleteMapping
    public ApiResponse<Void> unblockAll(
            @AuthenticationPrincipal User me
    ) {
        userBlockService.unblockAll(me.getId());
        return ApiResponse.onSuccess(null);
    }
}
