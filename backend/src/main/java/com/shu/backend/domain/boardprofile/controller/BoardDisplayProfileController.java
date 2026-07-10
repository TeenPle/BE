package com.shu.backend.domain.boardprofile.controller;

import com.shu.backend.domain.boardprofile.dto.BoardDisplayProfileDTO;
import com.shu.backend.domain.boardprofile.service.BoardDisplayProfileService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.status.UserSuccessStatus;
import com.shu.backend.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/board-profiles")
public class BoardDisplayProfileController {

    private final BoardDisplayProfileService boardDisplayProfileService;

    @GetMapping
    public ApiResponse<List<BoardDisplayProfileDTO.Response>> getMyBoardProfiles(
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.of(
                UserSuccessStatus.USER_BOARD_PROFILES_SUCCESS,
                boardDisplayProfileService.getMyProfiles(user.getId())
        );
    }

    @PatchMapping(value = "/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BoardDisplayProfileDTO.Response> updateMyBoardProfile(
            @AuthenticationPrincipal User user,
            @PathVariable Long boardId,
            @RequestPart("data") BoardDisplayProfileDTO.UpdateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        MultipartFile file = files == null || files.isEmpty() ? null : files.getFirst();
        return ApiResponse.of(
                UserSuccessStatus.USER_BOARD_PROFILE_UPDATE_SUCCESS,
                boardDisplayProfileService.updateMyProfile(user.getId(), boardId, request.getDisplayName(), file)
        );
    }
}
