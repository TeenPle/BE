package com.shu.backend.domain.adminaudit.service;

import com.shu.backend.domain.adminaudit.dto.AdminAuditLogResponse;
import com.shu.backend.domain.adminaudit.entity.AdminAuditLog;
import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.adminaudit.repository.AdminAuditLogRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminAuditLogResponse> getLogs(
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            Long adminId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        Specification<AdminAuditLog> spec = (root, query, cb) -> cb.conjunction();
        if (action != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (targetType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetType"), targetType));
        }
        if (adminId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("admin").get("id"), adminId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to));
        }
        return adminAuditLogRepository.findAll(spec, pageable)
                .map(AdminAuditLogResponse::from);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            Long adminId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            Long targetId,
            String reason,
            String metadata
    ) {
        if (adminId == null || targetId == null) {
            return;
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        adminAuditLogRepository.save(AdminAuditLog.builder()
                .admin(admin)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .reason(normalize(reason))
                .metadata(metadata)
                .build());
    }

    private String normalize(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }
}
