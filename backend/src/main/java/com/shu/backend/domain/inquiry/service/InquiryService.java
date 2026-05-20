package com.shu.backend.domain.inquiry.service;

import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.adminaudit.service.AdminAuditLogService;
import com.shu.backend.domain.inquiry.dto.InquiryDTO;
import com.shu.backend.domain.inquiry.entity.Inquiry;
import com.shu.backend.domain.inquiry.enums.InquiryStatus;
import com.shu.backend.domain.inquiry.exception.InquiryException;
import com.shu.backend.domain.inquiry.exception.status.InquiryErrorStatus;
import com.shu.backend.domain.inquiry.repository.InquiryRepository;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.notification.service.NotificationService;
import com.shu.backend.domain.push.service.PushService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.global.util.PageRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PushService pushService;
    private final AdminAuditLogService adminAuditLogService;

    @Transactional
    public Long create(Long userId, InquiryDTO.CreateRequest request) {
        validateCreateRequest(request);
        User user = userRepository.findById(userId).orElseThrow();
        Inquiry inquiry = inquiryRepository.save(
                Inquiry.create(user, request.getTitle(), request.getContent()));
        return inquiry.getId();
    }

    public Page<InquiryDTO.SummaryResponse> getMyInquiries(Long userId, int page, int size) {
        Pageable pageable = PageRequestUtils.of(page, size, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
        return inquiryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(InquiryDTO.SummaryResponse::from);
    }

    public InquiryDTO.DetailResponse getMyInquiry(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findDetailByIdAndUserId(inquiryId, userId)
                .orElseThrow(() -> new InquiryException(InquiryErrorStatus.INQUIRY_NOT_FOUND));
        return InquiryDTO.DetailResponse.from(inquiry);
    }

    public Page<InquiryDTO.SummaryResponse> getAdminInquiries(InquiryStatus status, int page, int size) {
        Pageable pageable = PageRequestUtils.of(page, size, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
        return inquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(InquiryDTO.SummaryResponse::from);
    }

    public InquiryDTO.DetailResponse getAdminInquiry(Long adminId, Long inquiryId) {
        if (adminId == null) {
            throw new InquiryException(InquiryErrorStatus.INQUIRY_NOT_FOUND);
        }
        Inquiry inquiry = inquiryRepository.findDetailById(inquiryId)
                .orElseThrow(() -> new InquiryException(InquiryErrorStatus.INQUIRY_NOT_FOUND));
        adminAuditLogService.record(
                adminId,
                AdminAuditAction.VIEW_INQUIRY_DETAIL,
                AdminAuditTargetType.INQUIRY,
                inquiryId,
                "문의 상세 열람",
                inquiryMetadata(inquiry)
        );
        return InquiryDTO.DetailResponse.from(inquiry);
    }

    @Transactional
    public Long answer(Long adminId, Long inquiryId, String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            throw new InquiryException(InquiryErrorStatus.INQUIRY_ANSWER_REQUIRED);
        }
        Inquiry inquiry = inquiryRepository.findDetailById(inquiryId)
                .orElseThrow(() -> new InquiryException(InquiryErrorStatus.INQUIRY_NOT_FOUND));
        if (inquiry.getStatus() == InquiryStatus.ANSWERED) {
            throw new InquiryException(InquiryErrorStatus.INQUIRY_ALREADY_ANSWERED);
        }
        User admin = userRepository.findById(adminId).orElseThrow();
        inquiry.answer(admin, answer);

        adminAuditLogService.recordAfterCommit(
                adminId,
                AdminAuditAction.ANSWER_INQUIRY,
                AdminAuditTargetType.INQUIRY,
                inquiryId,
                "문의 답변 등록",
                inquiryMetadata(inquiry)
        );

        // 문의 답변은 사용자가 놓치면 안 되는 운영 알림이므로 앱 알림과 푸시를 함께 발송한다.
        String message = "문의하신 내용에 답변이 등록되었습니다.";
        Long notificationId = notificationService.create(
                NotificationType.INQUIRY,
                NotificationTargetType.INQUIRY,
                inquiry.getId(),
                message,
                inquiry.getUser().getId(),
                adminId
        );

        if (notificationId != null) {
            pushService.sendToUserAfterCommit(
                    inquiry.getUser().getId(),
                    "문의 답변",
                    message,
                    Map.of(
                            "notificationId", String.valueOf(notificationId),
                            "type", NotificationType.INQUIRY.name(),
                            "targetType", NotificationTargetType.INQUIRY.name(),
                            "targetId", String.valueOf(inquiry.getId())
                    )
            );
        }

        return inquiry.getId();
    }

    private String inquiryMetadata(Inquiry inquiry) {
        return "userId=" + inquiry.getUser().getId();
    }

    private void validateCreateRequest(InquiryDTO.CreateRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new InquiryException(InquiryErrorStatus.INQUIRY_TITLE_REQUIRED);
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new InquiryException(InquiryErrorStatus.INQUIRY_CONTENT_REQUIRED);
        }
    }
}
