package com.shu.backend.domain.ad.service;

import com.shu.backend.domain.ad.dto.AdBannerRequest;
import com.shu.backend.domain.ad.dto.AdBannerResponse;
import com.shu.backend.domain.ad.entity.AdBanner;
import com.shu.backend.domain.ad.enums.AdPlacement;
import com.shu.backend.domain.ad.repository.AdBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdBannerService {

    private final AdBannerRepository adBannerRepository;

    @Transactional(readOnly = true)
    public AdBannerResponse getActive(AdPlacement placement) {
        return adBannerRepository.findFirstActive(placement, LocalDateTime.now())
                .map(AdBannerResponse::from)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AdBannerResponse> getAdminAds() {
        return adBannerRepository.findAllByOrderByPlacementAscPriorityAscIdDesc()
                .stream()
                .map(AdBannerResponse::from)
                .toList();
    }

    @Transactional
    public Long create(AdBannerRequest request) {
        return adBannerRepository.save(AdBanner.create(request)).getId();
    }

    @Transactional
    public Long update(Long adId, AdBannerRequest request) {
        AdBanner ad = adBannerRepository.findById(adId)
                .orElseThrow(() -> new IllegalArgumentException("광고를 찾을 수 없습니다."));
        ad.apply(request);
        return ad.getId();
    }

    @Transactional
    public void delete(Long adId) {
        adBannerRepository.deleteById(adId);
    }
}
