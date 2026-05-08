package com.shu.backend.domain.adminaudit.entity;

import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "admin_audit_log",
        indexes = {
                @Index(name = "idx_admin_audit_admin_created", columnList = "admin_id, created_at"),
                @Index(name = "idx_admin_audit_target", columnList = "target_type, target_id"),
                @Index(name = "idx_admin_audit_action_created", columnList = "action, created_at")
        }
)
public class AdminAuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private AdminAuditTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(length = 500)
    private String reason;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
