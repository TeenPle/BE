package com.shu.backend.domain.school.controller;

import com.shu.backend.domain.post.dto.PostResponse;
import com.shu.backend.domain.school.dto.SchoolCreateRequest;
import com.shu.backend.domain.school.dto.SchoolDetailResponse;
import com.shu.backend.domain.school.dto.SchoolNeisUpdateRequest;
import com.shu.backend.domain.school.dto.SchoolResponse;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.exception.status.SchoolSuccessStatus;
import com.shu.backend.domain.school.service.SchoolService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import com.shu.backend.global.neis.NeisSyncResult;
import com.shu.backend.global.neis.NeisSchoolSyncService;
import com.shu.backend.global.util.PageRequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "School", description = "학교 관련 API")
@RestController
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;
    private final NeisSchoolSyncService neisSchoolSyncService;

    @Operation(
            summary = "학교생성",
            description = "학교명을 입력 받아 학교를 생성합니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/admin/schools", consumes = "application/json", produces = "application/json")
    public ApiResponse<Long> createSchool(@Valid @RequestBody SchoolCreateRequest schoolCreateRequest) {
        Long id = schoolService.createSchool(schoolCreateRequest);

        return ApiResponse.of(SchoolSuccessStatus.SCHOOL_CREATE_SUCCESS, id);
    }

    @Operation(
            summary = "지역별 학교 조회",
            description = "지역별로 학교를 조회할 수 있으며, 학교명 검색 기능을 통해 특정 학교를 조회할 수 있습니다."
    )
    @GetMapping(value = "/api/regions/{regionId}/schools")
    public ApiResponse<List<SchoolResponse>> getSchoolsByRegion(
            @PathVariable Long regionId,
            @RequestParam(required = false) String keyword
    ){
        List<School> schools = schoolService.getSchoolsByRegion(regionId, keyword);

        List<SchoolResponse> responses = schools.stream()
                .map(SchoolResponse::toDto)
                .toList();

        return ApiResponse.of(SchoolSuccessStatus.SCHOOL_FOUND, responses);
    }

    @Operation(
            summary = "학교 검색",
            description = "회원가입 시 학교명을 기반으로 학교를 검색합니다."
    )
    @GetMapping(value = "/api/schools/search")
    public ApiResponse<List<SchoolResponse>> searchSchools(
            @RequestParam String keyword
    ){
        List<School> schools = schoolService.searchSchools(keyword);

        List<SchoolResponse> responses = schools.stream()
                .map(SchoolResponse::toDto)
                .toList();

        return ApiResponse.of(SchoolSuccessStatus.SCHOOL_FOUND, responses);
    }

    @Operation(
            summary = "학교 전체 게시판 최신글 조회",
            description = "학교 내 모든 게시판(학교+지역)의 최신 게시글을 페이지 단위로 조회합니다. '전체' 탭에 사용됩니다."
    )
    @GetMapping(value = "/api/schools/{schoolId}/posts")
    public ApiResponse<Slice<PostResponse>> getAllPostsBySchool(
            @PathVariable Long schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user
    ) {
        Pageable pageable = PageRequestUtils.of(page, size, 50);
        Slice<PostResponse> posts = schoolService.getAllPostsBySchool(schoolId, pageable, user.getId());
        return ApiResponse.onSuccess(posts);
    }

    @Operation(
            summary = "학교 상세 조회",
            description = "특정 학교 접속 시의 메인 화면이며, 학교 상세 정보와 기본 화면으로 자유 게시판의 글 목록을 조회합니다."
    )
    @GetMapping(value = "/api/schools/{schoolId}")
    public ApiResponse<SchoolDetailResponse> getSchoolDetail(
            @PathVariable Long schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user
    ){
        Pageable pageable = PageRequestUtils.of(page, size, 50);

        SchoolDetailResponse response = schoolService.getSchoolDetail(schoolId, "자유게시판", pageable, user.getId());

        return ApiResponse.of(SchoolSuccessStatus.SCHOOL_FOUND, response);
    }

    @Operation(
            summary = "학교 NEIS 코드 등록",
            description = "학교의 NEIS 시도교육청코드와 행정표준코드를 수동으로 등록합니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/schools/{schoolId}/neis")
    public ApiResponse<Void> updateNeisCodes(
            @PathVariable Long schoolId,
            @Valid @RequestBody SchoolNeisUpdateRequest request
    ) {
        schoolService.updateNeisCodes(schoolId, request);
        return ApiResponse.of(SchoolSuccessStatus.SCHOOL_CREATE_SUCCESS, null);
    }

    @Operation(
            summary = "전체 학교 NEIS 코드 일괄 자동 동기화",
            description = "NEIS 코드가 없는 모든 학교를 학교명으로 NEIS API에서 검색해 자동 등록합니다. " +
                    "동명 학교가 2개 이상이면 ambiguous 카운트에 집계되며 수동 등록이 필요합니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/schools/sync-neis")
    public ApiResponse<NeisSyncResult> syncAllNeisCodes() {
        NeisSyncResult result = neisSchoolSyncService.syncAllMissing();
        return ApiResponse.onSuccess(result);
    }
}
