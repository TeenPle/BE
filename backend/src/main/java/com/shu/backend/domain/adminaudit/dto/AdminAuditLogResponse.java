package com.shu.backend.domain.adminaudit.dto;

import com.shu.backend.domain.adminaudit.entity.AdminAuditLog;
import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminAuditLogResponse {

    private Long id;
    private Long adminId;
    private String adminNickname;
    private AdminAuditAction action;
    private AdminAuditTargetType targetType;
    private Long targetId;
    private String reason;
    private String metadata;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    public static AdminAuditLogResponse from(AdminAuditLog log) {
        return AdminAuditLogResponse.builder()
                .id(log.getId())
                .adminId(log.getAdmin().getId())
                .adminNickname(log.getAdmin().getNickname())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .reason(log.getReason())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
