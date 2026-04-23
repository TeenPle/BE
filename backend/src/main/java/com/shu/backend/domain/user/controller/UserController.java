package com.shu.backend.domain.user.controller;

import com.shu.backend.domain.user.dto.UserDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.status.UserSuccessStatus;
import com.shu.backend.domain.user.service.UserService;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ApiResponse<UserDTO.ProfileResponse> getMyProfile(
            @AuthenticationPrincipal User user
    ) {
        return ApiResponse.of(
                UserSuccessStatus.USER_PROFILE_SUCCESS,
                userService.getMyProfile(user.getId())
        );
    }

    @Operation(summary = "닉네임 변경")
    @PatchMapping("/me/nickname")
    public ApiResponse<Void> updateNickname(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid UserDTO.NicknameUpdateRequest request
    ) {
        userService.updateNickname(user.getId(), request.getNickname());
        return ApiResponse.of(UserSuccessStatus.USER_NICKNAME_UPDATE_SUCCESS, null);
    }

    @Operation(summary = "비밀번호 변경")
    @PatchMapping("/me/password")
    public ApiResponse<Void> updatePassword(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid UserDTO.PasswordUpdateRequest request
    ) {
        userService.updatePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.of(UserSuccessStatus.USER_PASSWORD_UPDATE_SUCCESS, null);
    }

    @Operation(summary = "내가 쓴 글 목록")
    @GetMapping("/me/posts")
    public ApiResponse<List<UserDTO.MyPostResponse>> getMyPosts(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.of(
                UserSuccessStatus.USER_MY_POSTS_SUCCESS,
                userService.getMyPosts(user.getId(), page, size)
        );
    }

    @Operation(summary = "내가 쓴 댓글 목록")
    @GetMapping("/me/comments")
    public ApiResponse<List<UserDTO.MyCommentResponse>> getMyComments(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.of(
                UserSuccessStatus.USER_MY_COMMENTS_SUCCESS,
                userService.getMyComments(user.getId(), page, size)
        );
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount(
            @AuthenticationPrincipal User user
    ) {
        userService.deleteAccount(user.getId());
        return ApiResponse.of(UserSuccessStatus.USER_DELETE_SUCCESS, null);
    }
}
