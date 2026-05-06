package com.shu.backend.domain.poll.controller;

import com.shu.backend.domain.poll.dto.PollResponse;
import com.shu.backend.domain.poll.dto.PollVoteRequest;
import com.shu.backend.domain.poll.exception.status.PollSuccessStatus;
import com.shu.backend.domain.poll.service.PollService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/poll")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping("/vote")
    public ApiResponse<PollResponse> vote(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user,
            @RequestBody @Valid PollVoteRequest request
    ) {
        PollResponse response = pollService.vote(postId, user.getId(), request.getOptionId());
        return ApiResponse.of(PollSuccessStatus.POLL_VOTE_SUCCESS, response);
    }
}
