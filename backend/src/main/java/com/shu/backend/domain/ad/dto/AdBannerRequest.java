package com.shu.backend.domain.ad.dto;

import com.shu.backend.domain.ad.enums.AdPlacement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AdBannerRequest(
        @NotNull AdPlacement placement,
        @NotBlank @Size(max = 80) String title,
        @NotBlank @Size(max = 160) String subtitle,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String linkUrl,
        boolean active,
        int priority,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
