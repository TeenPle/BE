package com.shu.backend.domain.inquiry.controller;

import com.shu.backend.domain.inquiry.dto.InquiryDTO;
import com.shu.backend.domain.inquiry.enums.InquiryStatus;
import com.shu.backend.domain.inquiry.service.InquiryService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inquiries")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public ApiResponse<Page<InquiryDTO.SummaryResponse>> getInquiries(
            @RequestParam(defaultValue = "PENDING") InquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(inquiryService.getAdminInquiries(status, page, size));
    }

    @GetMapping("/{inquiryId}")
    public ApiResponse<InquiryDTO.DetailResponse> getInquiry(@PathVariable Long inquiryId) {
        return ApiResponse.onSuccess(inquiryService.getAdminInquiry(inquiryId));
    }

    @PostMapping("/{inquiryId}/answer")
    public ApiResponse<Long> answer(
            @AuthenticationPrincipal User admin,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryDTO.AnswerRequest request
    ) {
        return ApiResponse.onSuccess(inquiryService.answer(admin.getId(), inquiryId, request.getAnswer()));
    }
}
