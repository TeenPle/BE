package com.shu.backend.domain.warning.controller;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.warning.dto.WarningDTO;
import com.shu.backend.domain.warning.service.WarningService;
import com.shu.backend.global.apiPayload.ApiResponse;
import com.shu.backend.global.util.PageRequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Warning", description = "경고 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/warnings")
public class WarningController {

    private final WarningService warningService;

    @Operation(
            summary = "미확인 경고 조회",
            description = "로그인한 사용자에게 읽지 않은 경고가 있으면 반환합니다. 없으면 null."
    )
    @GetMapping("/me/unread")
    public ApiResponse<WarningDTO.UnreadResponse> getUnread(
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.onSuccess(
                warningService.getUnreadWarning(user.getId()).orElse(null)
        );
    }

    @Operation(
            summary = "경고 읽음 처리",
            description = "경고 팝업을 확인한 후 읽음 상태로 변경합니다."
    )
    @PostMapping("/me/{warningId}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long warningId
    ) {
        warningService.markAsRead(warningId, user.getId());
        return ApiResponse.onSuccess(null);
    }

    @Operation(
            summary = "내 경고 이력 조회",
            description = "로그인한 사용자의 경고 이력을 최신순으로 페이지 단위로 조회합니다."
    )
    @GetMapping("/me")
    public ApiResponse<Page<WarningDTO.HistoryResponse>> getMyWarnings(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequestUtils.of(page, size, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.onSuccess(warningService.getMyWarnings(user.getId(), pageable));
    }
}
