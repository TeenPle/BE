package com.shu.backend.domain.penalty.controller;


import com.shu.backend.domain.penalty.dto.PenaltyDTO;
import com.shu.backend.domain.penalty.service.PenaltyService;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Admin Penalty",
        description = "관리자 제재 조회 API"
)
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.onSuccess(penaltyService.getPenaltiesByUser(userId, pageable));
    }
}
