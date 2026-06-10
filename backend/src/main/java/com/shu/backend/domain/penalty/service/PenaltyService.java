package com.shu.backend.domain.penalty.service;

import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.adminaudit.service.AdminAuditLogService;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.notification.service.NotificationService;
import com.shu.backend.domain.penalty.dto.PenaltyDTO;
import com.shu.backend.domain.push.service.PushService;
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
import java.util.Map;

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
    private final NotificationService notificationService;
    private final PushService pushService;

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

        // 제재는 사용자가 놓치면 안 되는 운영 알림이므로
        // 알림 설정과 무관하게 앱 알림과 푸시를 함께 발송한다. (경고/문의 답변과 동일한 정책)
        String message = "신고 처리 결과에 따라 커뮤니티 이용이 " + penaltyDays + "일간 제한되었어요. "
                + "제재 내역에서 자세한 내용을 확인해 주세요.";
        Long receiverUserId = report.getReportedUser().getId();
        // actorId는 null: 제재 주체(관리자)를 알림에 노출하지 않는 시스템 알림으로 처리한다.
        Long notificationId = notificationService.create(
                NotificationType.PENALTY,
                NotificationTargetType.PENALTY,
                penaltyId,
                message,
                receiverUserId,
                null
        );

        // 알림/푸시 실패가 제재 생성 자체를 막지 않도록 푸시는 별도로 보호한다.
        if (notificationId != null) {
            try {
                // 트랜잭션 커밋 후 발송 — 롤백 시 푸시만 나가는 상황을 방지한다.
                pushService.sendToUserAfterCommit(
                        receiverUserId,
                        "이용 제한 안내",
                        message,
                        Map.of(
                                "notificationId", String.valueOf(notificationId),
                                "type", NotificationType.PENALTY.name(),
                                "targetType", NotificationTargetType.PENALTY.name(),
                                "targetId", String.valueOf(penaltyId)
                        )
                );
            } catch (Exception e) {
                log.warn("Penalty push scheduling failed: notificationId={}, penaltyId={}, receiverUserId={}",
                        notificationId, penaltyId, receiverUserId, e);
            }
        }

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
