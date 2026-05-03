package com.shu.backend.domain.block.controller;

import com.shu.backend.domain.block.service.UserBlockService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public ApiResponse<List<Map<String, Object>>> getBlockedUsers(
            @AuthenticationPrincipal User me
    ) {
        List<User> blocked = userBlockService.getBlockedUsers(me.getId());
        List<Map<String, Object>> result = blocked.stream()
                .map(u -> Map.<String, Object>of(
                        "userId", u.getId(),
                        "nickname", u.getNickname(),
                        "profileImageUrl", u.getProfileImageUrl() != null ? u.getProfileImageUrl() : ""
                ))
                .collect(Collectors.toList());
        return ApiResponse.onSuccess(result);
    }
}
