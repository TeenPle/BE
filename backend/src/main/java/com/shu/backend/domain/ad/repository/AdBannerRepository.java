package com.shu.backend.domain.ad.repository;

import com.shu.backend.domain.ad.entity.AdBanner;
import com.shu.backend.domain.ad.enums.AdPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdBannerRepository extends JpaRepository<AdBanner, Long> {

    List<AdBanner> findAllByOrderByPlacementAscPriorityAscIdDesc();

    @Query("""
        select a
        from AdBanner a
        where a.placement = :placement
          and a.active = true
          and (a.startAt is null or a.startAt <= :now)
          and (a.endAt is null or a.endAt >= :now)
        order by a.priority asc, a.id desc
    """)
    List<AdBanner> findActiveCandidates(@Param("placement") AdPlacement placement, @Param("now") LocalDateTime now);

    default Optional<AdBanner> findFirstActive(AdPlacement placement, LocalDateTime now) {
        return findActiveCandidates(placement, now).stream().findFirst();
    }
}
