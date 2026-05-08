package com.shu.backend.domain.admin.controller;

import com.shu.backend.domain.admin.dto.AdminBoardResponse;
import com.shu.backend.domain.admin.dto.AdminPostDetailResponse;
import com.shu.backend.domain.admin.dto.AdminPostSummaryResponse;
import com.shu.backend.domain.admin.dto.AdminSchoolResponse;
import com.shu.backend.domain.admin.service.AdminContentService;
import com.shu.backend.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/content")
public class AdminContentController {

    private final AdminContentService adminContentService;

    @GetMapping("/schools")
    public ApiResponse<Page<AdminSchoolResponse>> searchSchools(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(adminContentService.searchSchools(keyword, page, size));
    }

    @GetMapping("/schools/{schoolId}/boards")
    public ApiResponse<List<AdminBoardResponse>> getBoardsBySchool(
            @PathVariable Long schoolId
    ) {
        return ApiResponse.onSuccess(adminContentService.getBoardsBySchool(schoolId));
    }

    @GetMapping("/boards/{boardId}/posts")
    public ApiResponse<Page<AdminPostSummaryResponse>> getPostsByBoard(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(adminContentService.getPostsByBoard(boardId, page, size));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<AdminPostDetailResponse> getPostDetail(
            @PathVariable Long postId
    ) {
        return ApiResponse.onSuccess(adminContentService.getPostDetail(postId));
    }
}
