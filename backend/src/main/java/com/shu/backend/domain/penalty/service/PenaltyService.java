package com.shu.backend.domain.penalty.service;

import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.adminaudit.service.AdminAuditLogService;
import com.shu.backend.domain.penalty.dto.PenaltyDTO;
import com.shu.backend.domain.penalty.entity.Penalty;
import com.shu.backend.domain.penalty.enums.PenaltyStatus;
import com.shu.backend.domain.penalty.exception.PenaltyException;
import com.shu.backend.domain.penalty.exception.status.PenaltyErrorStatus;
import com.shu.backend.domain.penalty.repository.PenaltyRepository;
import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.exception.ReportException;
import com.shu.backend.domain.report.exception.status.ReportErrorStatus;
import com.shu.backend.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PenaltyService {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final PenaltyRepository penaltyRepository;
    private final ReportRepository reportRepository;
    private final AdminAuditLogService adminAuditLogService;

    @Transactional
    public Long create(Long reportId, int penaltyDays){

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        if (penaltyRepository.existsByReportId(reportId)) {
            throw new ReportException(ReportErrorStatus.PENALTY_ALREADY_CREATED);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(penaltyDays);

        Penalty penalty = Penalty.builder()
                .report(report)
                .user(report.getReportedUser())
                .reason(report.getReportReason())
                .status(PenaltyStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();

        Long penaltyId = penaltyRepository.save(penalty).getId();
        log.info("Penalty created: penaltyId={}, reportId={}, userId={}, penaltyDays={}",
                penaltyId, reportId, report.getReportedUser().getId(), penaltyDays);
        return penaltyId;
    }

    public PenaltyDTO.MyActiveResponse getMyActivePenalty(Long myId) {
        return penaltyRepository.findTop1ByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(myId, LocalDateTime.now())
                .map(p -> PenaltyDTO.MyActiveResponse.builder()
                        .penalized(true)
                        .expiresAt(p.getExpiresAt().format(ISO_FMT))
                        .reason(p.getReason().name())
                        .reportId(p.getReport().getId())
                        .build())
                .orElseGet(() -> PenaltyDTO.MyActiveResponse.builder()
                        .penalized(false)
                        .build());
    }


    public Page<PenaltyDTO.SummaryResponse> getPenaltiesByUser(Long userId, Pageable pageable) {
        return penaltyRepository.findAllByUserId(userId, pageable)
                .map(PenaltyDTO.SummaryResponse::from);
    }

    public Page<PenaltyDTO.SummaryResponse> getAllPenalties(Pageable pageable) {
        return penaltyRepository.findAll(pageable)
                .map(PenaltyDTO.SummaryResponse::from);
    }

    @Transactional
    public void cancel(Long penaltyId) {
        cancel(null, penaltyId);
    }

    @Transactional
    public void cancel(Long adminId, Long penaltyId) {
        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new PenaltyException(PenaltyErrorStatus.PENALTY_NOT_FOUND));

        if (penalty.getStatus() != PenaltyStatus.ACTIVE) {
            throw new PenaltyException(PenaltyErrorStatus.PENALTY_NOT_ACTIVE);
        }

        penalty.cancel();
        adminAuditLogService.recordAfterCommit(
                adminId,
                AdminAuditAction.CANCEL_PENALTY,
                AdminAuditTargetType.PENALTY,
                penaltyId,
                "제재 취소",
                "reportId=" + penalty.getReport().getId()
        );
        log.info("Penalty cancelled: penaltyId={}, adminId={}, userId={}, reportId={}",
                penaltyId, adminId, penalty.getUser().getId(), penalty.getReport().getId());
    }

}
