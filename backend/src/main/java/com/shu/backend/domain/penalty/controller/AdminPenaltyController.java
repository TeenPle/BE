package com.shu.backend.domain.penalty.controller;


import com.shu.backend.domain.penalty.dto.PenaltyDTO;
import com.shu.backend.domain.penalty.service.PenaltyService;
import com.shu.backend.global.apiPayload.ApiResponse;
import com.shu.backend.global.util.PageRequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Tag(
        name = "Admin Penalty",
        description = "관리자 제재 조회 API"
)
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/penalties")
public class AdminPenaltyController {

    private final PenaltyService penaltyService;

    @Operation(
            summary = "전체 제재 목록 조회",
            description = "모든 유저의 제재 이력을 최신순으로 페이지 단위로 조회합니다."
    )
    @GetMapping
    public ApiResponse<Page<PenaltyDTO.SummaryResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequestUtils.of(page, size, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.onSuccess(penaltyService.getAllPenalties(pageable));
    }

    @Operation(
            summary = "유저별 제재 목록 조회",
            description = "특정 유저의 제재 이력을 페이지로 조회합니다."
    )
    @GetMapping("/user")
    public ApiResponse<Page<PenaltyDTO.SummaryResponse>> listByUser(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequestUtils.of(page, size, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.onSuccess(penaltyService.getPenaltiesByUser(userId, pageable));
    }

    @Operation(
            summary = "제재 취소",
            description = "활성 상태의 제재를 즉시 취소합니다."
    )
    @PostMapping("/{penaltyId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long penaltyId) {
        penaltyService.cancel(penaltyId);
        return ApiResponse.onSuccess(null);
    }
}
