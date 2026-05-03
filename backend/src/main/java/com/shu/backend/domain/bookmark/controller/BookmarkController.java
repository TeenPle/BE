package com.shu.backend.domain.bookmark.controller;

import com.shu.backend.domain.bookmark.dto.BookmarkResult;
import com.shu.backend.domain.bookmark.dto.BookmarkedPostResponse;
import com.shu.backend.domain.bookmark.exception.status.BookmarkSuccessStatus;
import com.shu.backend.domain.bookmark.service.BookmarkService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bookmark", description = "북마크 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "북마크 토글", description = "게시글 북마크를 추가하거나 해제합니다.")
    @PostMapping("/posts/{postId}/bookmarks/toggle")
    public ApiResponse<BookmarkResult> toggle(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user) {

        BookmarkResult result = bookmarkService.toggle(user.getId(), postId);
        BookmarkSuccessStatus status = result.isBookmarked()
                ? BookmarkSuccessStatus.BOOKMARK_ADDED
                : BookmarkSuccessStatus.BOOKMARK_REMOVED;
        return ApiResponse.of(status, result);
    }

    @Operation(summary = "내 북마크 목록 조회", description = "북마크한 게시글 목록을 최신 북마크 순으로 조회합니다.")
    @GetMapping("/users/me/bookmarks")
    public ApiResponse<List<BookmarkedPostResponse>> getMyBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {

        List<BookmarkedPostResponse> result = bookmarkService.getMyBookmarks(user.getId(), page, size);
        return ApiResponse.of(BookmarkSuccessStatus.BOOKMARK_LIST_FETCHED, result);
    }
}
