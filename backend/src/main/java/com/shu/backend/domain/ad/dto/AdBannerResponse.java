package com.shu.backend.domain.ad.dto;

import com.shu.backend.domain.ad.entity.AdBanner;
import com.shu.backend.domain.ad.enums.AdPlacement;

import java.time.LocalDateTime;

public record AdBannerResponse(
        Long id,
        AdPlacement placement,
        String title,
        String subtitle,
        String imageUrl,
        String linkUrl,
        boolean active,
        int priority,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
    public static AdBannerResponse from(AdBanner ad) {
        return new AdBannerResponse(
                ad.getId(),
                ad.getPlacement(),
                ad.getTitle(),
                ad.getSubtitle(),
                ad.getImageUrl(),
                ad.getLinkUrl(),
                ad.isActive(),
                ad.getPriority(),
                ad.getStartAt(),
                ad.getEndAt()
        );
    }
}
