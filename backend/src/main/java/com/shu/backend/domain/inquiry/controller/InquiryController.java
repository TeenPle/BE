package com.shu.backend.domain.inquiry.controller;

import com.shu.backend.domain.inquiry.dto.InquiryDTO;
import com.shu.backend.domain.inquiry.service.InquiryService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import com.shu.backend.global.ratelimit.RateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    @RateLimit(key = "inquiry:create", limit = 3, windowSeconds = 600)
    public ApiResponse<Long> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody InquiryDTO.CreateRequest request
    ) {
        return ApiResponse.onSuccess(inquiryService.create(user.getId(), request));
    }

    @GetMapping
    public ApiResponse<Page<InquiryDTO.SummaryResponse>> getMyInquiries(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(inquiryService.getMyInquiries(user.getId(), page, size));
    }

    @GetMapping("/{inquiryId}")
    public ApiResponse<InquiryDTO.DetailResponse> getMyInquiry(
            @AuthenticationPrincipal User user,
            @PathVariable Long inquiryId
    ) {
        return ApiResponse.onSuccess(inquiryService.getMyInquiry(user.getId(), inquiryId));
    }
}
