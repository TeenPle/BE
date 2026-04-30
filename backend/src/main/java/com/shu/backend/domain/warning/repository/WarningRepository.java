package com.shu.backend.domain.warning.repository;

import com.shu.backend.domain.warning.entity.Warning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WarningRepository extends JpaRepository<Warning, Long> {

    boolean existsByReportId(Long reportId);

    Optional<Warning> findTop1ByUserIdAndIsReadFalseOrderByCreatedAtAsc(Long userId);
}
