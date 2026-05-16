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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
                .ipAddress(resolveClientIp())
                .userAgent(resolveUserAgent())
                .build());
    }

    public void recordAfterCommit(
            Long adminId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            Long targetId,
            String reason,
            String metadata
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            record(adminId, action, targetType, targetId, reason, metadata);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                record(adminId, action, targetType, targetId, reason, metadata);
            }
        });
    }

    private String normalize(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private String resolveClientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        String forwardedFor = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }

        String realIp = normalizeHeaderValue(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        return normalizeHeaderValue(request.getRemoteAddr());
    }

    private String resolveUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String userAgent = normalizeHeaderValue(request.getHeader("User-Agent"));
        return userAgent != null && userAgent.length() > 500
                ? userAgent.substring(0, 500)
                : userAgent;
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String firstForwardedIp(String header) {
        String value = normalizeHeaderValue(header);
        if (value == null) {
            return null;
        }
        String first = value.split(",")[0].trim();
        return normalizeHeaderValue(first);
    }

    private String normalizeHeaderValue(String value) {
        if (value == null || value.isBlank() || "unknown".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }
}
