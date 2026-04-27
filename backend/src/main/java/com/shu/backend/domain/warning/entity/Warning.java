package com.shu.backend.domain.warning.entity;

import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "warning",
        indexes = {
                @Index(name = "idx_warning_user_read", columnList = "user_id,is_read")
        }
)
public class Warning extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(length = 500)
    private String adminComment;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    public void markAsRead() {
        this.isRead = true;
    }
}
