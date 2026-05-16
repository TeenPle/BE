package com.shu.backend.domain.warning.service;

import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.adminaudit.service.AdminAuditLogService;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.global.firebase.FcmSender;
import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.enums.ReportStatus;
import com.shu.backend.domain.report.enums.TargetType;
import com.shu.backend.domain.report.exception.ReportException;
import com.shu.backend.domain.report.exception.status.ReportErrorStatus;
import com.shu.backend.domain.report.repository.ReportRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.warning.dto.WarningDTO;
import com.shu.backend.domain.warning.entity.Warning;
import com.shu.backend.domain.warning.exception.WarningException;
import com.shu.backend.domain.warning.exception.status.WarningErrorStatus;
import com.shu.backend.domain.warning.repository.WarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarningService {

    private final WarningRepository warningRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final FcmSender fcmSender;
    private final AdminAuditLogService adminAuditLogService;

    @Transactional
    public Long issue(Long adminId, Long reportId, String adminComment) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ReportException(ReportErrorStatus.REPORT_NOT_PENDING);
        }

        if (warningRepository.existsByReportId(reportId)) {
            throw new WarningException(WarningErrorStatus.WARNING_ALREADY_ISSUED);
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        report.warn(admin);

        Warning warning = Warning.builder()
                .user(report.getReportedUser())
                .report(report)
                .adminComment(adminComment)
                .build();

        Long warningId = warningRepository.save(warning).getId();

        adminAuditLogService.recordAfterCommit(
                adminId,
                AdminAuditAction.WARN_REPORT,
                AdminAuditTargetType.REPORT,
                reportId,
                adminComment,
                "warningId=" + warningId
        );

        fcmSender.sendToUser(
                report.getReportedUser().getId(),
                "경고 발령",
                "관리자로부터 경고가 발령되었습니다. 앱을 열어 확인해주세요."
        );

        return warningId;
    }

    public Optional<WarningDTO.UnreadResponse> getUnreadWarning(Long userId) {
        return warningRepository
                .findTop1ByUserIdAndIsReadFalseOrderByCreatedAtAsc(userId)
                .map(w -> {
                    Report report = w.getReport();
                    String targetType = report.getTargetType().name();
                    String targetSummary = resolveTargetSummary(report.getTargetType(), report.getTargetId());
                    return WarningDTO.UnreadResponse.from(w, targetType, targetSummary);
                });
    }

    public Page<WarningDTO.HistoryResponse> getMyWarnings(Long userId, Pageable pageable) {
        return warningRepository.findAllByUserId(userId, pageable)
                .map(w -> WarningDTO.HistoryResponse.from(
                        w, resolveTargetSummary(w.getReport().getTargetType(), w.getReport().getTargetId())));
    }

    public Page<WarningDTO.HistoryResponse> getWarningsByUser(Long userId, Pageable pageable) {
        return warningRepository.findAllByUserId(userId, pageable)
                .map(w -> WarningDTO.HistoryResponse.from(
                        w, resolveTargetSummary(w.getReport().getTargetType(), w.getReport().getTargetId())));
    }

    public Page<WarningDTO.HistoryResponse> getAllWarnings(Pageable pageable) {
        return warningRepository.findAll(pageable)
                .map(w -> WarningDTO.HistoryResponse.from(
                        w, resolveTargetSummary(w.getReport().getTargetType(), w.getReport().getTargetId())));
    }

    private String resolveTargetSummary(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .map(p -> truncate(p.getTitle(), 80))
                    .orElse("(삭제된 게시글)");
            case COMMENT -> commentRepository.findById(targetId)
                    .map(c -> truncate(c.getContent(), 80))
                    .orElse("(삭제된 댓글)");
            default -> "";
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    @Transactional
    public void markAsRead(Long warningId, Long userId) {
        Warning warning = warningRepository.findById(warningId)
                .orElseThrow(() -> new WarningException(WarningErrorStatus.WARNING_NOT_FOUND));

        if (!warning.getUser().getId().equals(userId)) {
            throw new WarningException(WarningErrorStatus.WARNING_ACCESS_DENIED);
        }

        warning.markAsRead();
    }
}
