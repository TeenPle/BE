package com.shu.backend.domain.ad.entity;

import com.shu.backend.domain.ad.dto.AdBannerRequest;
import com.shu.backend.domain.ad.enums.AdPlacement;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = {
        @Index(name = "idx_ad_banner_active_placement", columnList = "placement, active, priority"),
        @Index(name = "idx_ad_banner_period", columnList = "start_at, end_at")
})
public class AdBanner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdPlacement placement;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 160)
    private String subtitle;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String linkUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int priority = 100;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    public static AdBanner create(AdBannerRequest request) {
        AdBanner ad = new AdBanner();
        ad.apply(request);
        return ad;
    }

    public void apply(AdBannerRequest request) {
        this.placement = request.placement();
        this.title = request.title().trim();
        this.subtitle = request.subtitle().trim();
        this.imageUrl = trimToNull(request.imageUrl());
        this.linkUrl = trimToNull(request.linkUrl());
        this.active = request.active();
        this.priority = request.priority();
        this.startAt = request.startAt();
        this.endAt = request.endAt();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
