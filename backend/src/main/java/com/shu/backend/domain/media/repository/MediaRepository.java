package com.shu.backend.domain.media.repository;

import com.shu.backend.domain.media.entity.Media;
import com.shu.backend.domain.media.enums.MediaTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByTargetTypeAndTargetIdIn(MediaTargetType targetType, List<Long> targetIds);
    List<Media> findByTargetTypeAndTargetId(MediaTargetType targetType, Long targetId);
    Optional<Media> findByIdAndUploaderId(Long id, Long uploaderId);
}
