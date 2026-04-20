package com.shu.backend.domain.post.controller;

import com.shu.backend.domain.post.dto.PostCreateRequest;
import com.shu.backend.domain.post.dto.PostDetailResponse;
import com.shu.backend.domain.post.dto.PostResponse;
import com.shu.backend.domain.post.dto.PostUpdateRequest;
import com.shu.backend.domain.post.exception.status.PostSuccessStatus;
import com.shu.backend.domain.post.service.PostService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
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
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.web.PageableDefault;
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

    private final PostService postService;

    @Operation(summary = "게시글 생성", description = "특정 게시판에 게시글을 작성합니다. 파일은 선택적으로 첨부할 수 있습니다.")
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)
            )
    )
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
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size, JpaSort.unsafe(Sort.Direction.fromString(sortDirection), sortBy));
        Slice<PostResponse> postResponses = postService.getPostsByBoardId(boardId, pageable);
        return ApiResponse.onSuccess(postResponses);
    }

    @GetMapping("/search")
    public ApiResponse<Slice<PostResponse>> searchPosts(
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user
    ) {
        Long schoolId = user.getSchool().getId();
        Long regionId = 1L;

        Slice<PostResponse> postResponses = postService.searchAccessiblePosts(schoolId, regionId, keyword, pageable);
        return ApiResponse.onSuccess(postResponses);
    }
}
