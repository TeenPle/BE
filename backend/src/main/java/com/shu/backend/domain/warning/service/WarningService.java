package com.shu.backend.domain.warning.service;

import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.enums.ReportStatus;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarningService {

    private final WarningRepository warningRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

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

        return warningRepository.save(warning).getId();
    }

    public Optional<WarningDTO.UnreadResponse> getUnreadWarning(Long userId) {
        return warningRepository
                .findTop1ByUserIdAndIsReadFalseOrderByCreatedAtAsc(userId)
                .map(WarningDTO.UnreadResponse::from);
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
