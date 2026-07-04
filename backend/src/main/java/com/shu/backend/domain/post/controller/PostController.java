package com.shu.backend.domain.post.controller;

import com.shu.backend.domain.post.dto.PostCreateRequest;
import com.shu.backend.domain.post.dto.PostDetailResponse;
import com.shu.backend.domain.post.dto.PostResponse;
import com.shu.backend.domain.post.dto.PostUpdateRequest;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.exception.status.PostSuccessStatus;
import com.shu.backend.domain.post.service.PostService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import com.shu.backend.global.ratelimit.RateLimit;
import com.shu.backend.global.util.PageRequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Post", description = "게시글 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "likeCount", "commentCount", "viewCount");

    private final PostService postService;

    @Operation(summary = "게시글 생성", description = "특정 게시판에 게시글을 작성합니다. 파일은 선택적으로 첨부할 수 있습니다.")
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)
            )
    )
    @RateLimit(key = "post:create:success", limit = 10, windowSeconds = 3600, countFailures = false)
    @PostMapping(value = "/boards/{boardId}/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> createPost(
            @PathVariable Long boardId,
            @AuthenticationPrincipal User user,
            @org.springframework.web.bind.annotation.RequestPart("data") @Valid PostCreateRequest req,
            @org.springframework.web.bind.annotation.RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        Long postId = postService.createPost(user.getId(), boardId, req, files);
        return ApiResponse.of(PostSuccessStatus.POST_CREATE_SUCCESS, postId);
    }

    @Operation(summary = "게시글 수정", description = "특정 게시글의 내용을 수정합니다. 새 파일을 추가하거나 기존 미디어를 삭제할 수 있습니다.")
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)
            )
    )
    @PatchMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> updatePost(
            @PathVariable Long postId,
            @org.springframework.web.bind.annotation.RequestPart("data") @Valid PostUpdateRequest req,
            @AuthenticationPrincipal User user,
            @org.springframework.web.bind.annotation.RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        Long id = postService.updatePost(postId, req, user.getId(), files);
        return ApiResponse.of(PostSuccessStatus.POST_UPDATE_SUCCESS, id);
    }

    @Operation(summary = "게시글 삭제", description = "특정 게시글을 삭제합니다.(soft delete)")
    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Long> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user
    ) {
        Long id = postService.deletePost(postId, user.getId());
        return ApiResponse.of(PostSuccessStatus.POST_DELETE_SUCCESS, id);
    }

    @Operation(summary = "게시글 상세 조회", description = "특정 게시글을 댓글 및 첨부파일을 포함하여 상세 조회합니다.")
    @GetMapping("/posts/{postId}")
    public ApiResponse<PostDetailResponse> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user
    ) {
        PostDetailResponse postDetail = postService.getPostDetail(postId, user.getId());
        return ApiResponse.onSuccess(postDetail);
    }

    @Operation(summary = "게시글 목록 조회", description = "특정 게시판의 게시글을 페이징하여 모두 조회합니다.")
    @GetMapping("/boards/{boardId}/posts")
    public ApiResponse<Slice<PostResponse>> getPostsByBoardId(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal User user
    ) {
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction safeDirection = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequestUtils.of(page, size, 50, Sort.by(safeDirection, safeSortBy));
        Slice<PostResponse> postResponses = postService.getPostsByBoardId(boardId, pageable, user.getId());
        return ApiResponse.onSuccess(postResponses);
    }

    @Operation(summary = "HOT 게시글 조회", description = "해당 학교의 좋아요 많은 순 상위 게시글 조회. filter: TODAY / WEEK(기본) / ALL")
    @GetMapping("/schools/{schoolId}/posts/hot")
    public ApiResponse<List<PostResponse>> getHotPosts(
            @PathVariable Long schoolId,
            @RequestParam(defaultValue = "WEEK") String filter,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user
    ) {
        List<PostResponse> hotPosts = postService.getHotPosts(schoolId, filter, size, user.getId());
        return ApiResponse.onSuccess(hotPosts);
    }

    @Operation(summary = "피드 상단 추천 게시글 조회", description = "기본 3시간 내 추천수가 높은 접근 가능 게시글을 조회합니다.")
    @GetMapping("/schools/{schoolId}/posts/top-recommended")
    public ApiResponse<List<PostResponse>> getTopRecommendedPosts(
            @PathVariable Long schoolId,
            @RequestParam(defaultValue = "3") int hours,
            @RequestParam(defaultValue = "3") int size,
            @AuthenticationPrincipal User user
    ) {
        List<PostResponse> topPosts = postService.getTopRecommendedPosts(schoolId, hours, size, user.getId());
        return ApiResponse.onSuccess(topPosts);
    }

    @GetMapping("/search")
    public ApiResponse<Slice<PostResponse>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(required = false) Long boardId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user
    ) {
        if (user.getSchool() == null) {
            throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
        }
        Long schoolId = user.getSchool().getId();
        Long regionId = (user.getSchool().getRegion() != null)
                ? user.getSchool().getRegion().getId()
                : null;

        Slice<PostResponse> postResponses = postService.searchAccessiblePosts(schoolId, regionId, boardId, keyword, pageable, user.getId());
        return ApiResponse.onSuccess(postResponses);
    }
}
