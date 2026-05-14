package com.shu.backend.domain.inquiry.entity;

import com.shu.backend.domain.inquiry.enums.InquiryStatus;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        indexes = {
                @Index(name = "idx_inquiry_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_inquiry_status_created", columnList = "status, created_at")
        }
)
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.PENDING;

    @Column(name = "admin_answer", length = 2000)
    private String adminAnswer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private User answeredBy;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    /** 사용자 문의는 최초 접수 시 답변 대기 상태로만 생성한다. */
    public static Inquiry create(User user, String title, String content) {
        Inquiry inquiry = new Inquiry();
        inquiry.user = user;
        inquiry.title = title.trim();
        inquiry.content = content.trim();
        inquiry.status = InquiryStatus.PENDING;
        return inquiry;
    }

    /** 답변 등록은 단일 관리자 답변으로 종결되며, 재답변은 서비스 계층에서 차단한다. */
    public void answer(User admin, String answer) {
        this.adminAnswer = answer.trim();
        this.answeredBy = admin;
        this.answeredAt = LocalDateTime.now();
        this.status = InquiryStatus.ANSWERED;
    }
}
