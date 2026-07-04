package com.shu.backend.domain.report.repository;

import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.enums.ReportStatus;
import com.shu.backend.domain.report.enums.TargetType;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, TargetType targetType, Long targetId);

    Optional<Report> findByReporterIdAndTargetTypeAndTargetId(Long reporterId, TargetType targetType, Long targetId);

    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);

    @Query("""
            select r from Report r
            join r.reporter reporter
            join r.reportedUser reportedUser
            where r.status = :status
              and (
                :keyword is null
                or :keyword = ''
                or lower(reporter.nickname) like lower(concat('%', :keyword, '%'))
                or lower(reportedUser.nickname) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(r.reportDetail, '')) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Report> searchByStatus(
            @Param("status") ReportStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Report r where r.targetType = :targetType and r.targetId = :targetId")
    void deleteAllByTargetTypeAndTargetId(
            @Param("targetType") TargetType targetType,
            @Param("targetId") Long targetId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Report r where r.targetType = :targetType and r.targetId in :targetIds")
    void deleteAllByTargetTypeAndTargetIdIn(
            @Param("targetType") TargetType targetType,
            @Param("targetIds") List<Long> targetIds);
}
