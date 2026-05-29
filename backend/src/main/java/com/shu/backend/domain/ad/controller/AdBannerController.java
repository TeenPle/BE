package com.shu.backend.domain.ad.controller;

import com.shu.backend.domain.ad.dto.AdBannerRequest;
import com.shu.backend.domain.ad.dto.AdBannerResponse;
import com.shu.backend.domain.ad.enums.AdPlacement;
import com.shu.backend.domain.ad.service.AdBannerService;
import com.shu.backend.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdBannerController {

    private final AdBannerService adBannerService;

    @GetMapping("/api/ads/active")
    public ApiResponse<AdBannerResponse> getActiveAd(@RequestParam AdPlacement placement) {
        return ApiResponse.onSuccess(adBannerService.getActive(placement));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/ads")
    public ApiResponse<List<AdBannerResponse>> getAdminAds() {
        return ApiResponse.onSuccess(adBannerService.getAdminAds());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/admin/ads")
    public ApiResponse<Long> createAd(@Valid @RequestBody AdBannerRequest request) {
        return ApiResponse.onSuccess(adBannerService.create(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/admin/ads/{adId}")
    public ApiResponse<Long> updateAd(
            @PathVariable Long adId,
            @Valid @RequestBody AdBannerRequest request
    ) {
        return ApiResponse.onSuccess(adBannerService.update(adId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/admin/ads/{adId}")
    public ApiResponse<Void> deleteAd(@PathVariable Long adId) {
        adBannerService.delete(adId);
        return ApiResponse.onSuccess(null);
    }
}
