package com.shu.backend.domain.verification.service;

import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.adminaudit.service.AdminAuditLogService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.verification.dto.VerificationAdminDTO;
import com.shu.backend.domain.verification.entity.UserSchoolVerification;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.verification.exception.VerificationErrorStatus;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRepository;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRequestRepository;
import com.shu.backend.domain.verification.status.VerificationStatus;
import com.shu.backend.global.exception.GeneralException;
import com.shu.backend.global.file.FileStorageService;
import com.shu.backend.global.util.PageRequestUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 학교 인증 요청에 대한 관리자 검토 업무를 처리하는 서비스.
 *
 * 관리자 측에서 인증 요청 목록/상세를 조회하고,
 * 요청을 승인 또는 거절하며,
 * 승인 시 최종 학교 인증 정보와 사용자 인증 상태를 반영한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SchoolVerificationAdminService {

    private final UserSchoolVerificationRequestRepository requestRepository;
    private final UserSchoolVerificationRepository verificationRepository;
    private final FileStorageService fileStorageService;
    private final AdminAuditLogService adminAuditLogService;

    // 상태별 학교 인증 요청 목록 조회
    @Transactional(readOnly = true)
    public List<VerificationAdminDTO.ListItemResponse> list(VerificationStatus status) {
        return requestRepository.findByStatusOrderByRequestedAtDesc(status)
                .stream()
                .map(this::toListItem)
                .toList();
    }

    /**
     * 상태별 학교 인증 요청을 20개 단위로 조회한다.
     *
     * 관리자 앱 목록은 스크롤 하단에서 다음 페이지를 이어 붙이므로,
     * 전체 요청을 한 번에 내려주지 않고 page/size 기반 Page 응답을 사용한다.
     */
    @Transactional(readOnly = true)
    public Page<VerificationAdminDTO.ListItemResponse> list(VerificationStatus status, int page, int size) {
        return list(status, null, page, size);
    }

    @Transactional(readOnly = true)
    public Page<VerificationAdminDTO.ListItemResponse> list(VerificationStatus status, String keyword, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "requestedAt");
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return requestRepository.findByStatus(
                    status,
                    PageRequestUtils.of(page, size, 50, sort)
            ).map(this::toListItem);
        }
        return requestRepository.searchByStatus(
                status,
                normalizedKeyword,
                PageRequestUtils.of(page, size, 50, sort)
        ).map(this::toListItem);
    }

    // 학교 인증 요청 상세 조회
    @Transactional(readOnly = true)
    public VerificationAdminDTO.DetailResponse detail(Long requestId) {
        Long adminUserId = getCurrentAdminUserId();
        UserSchoolVerificationRequest req = requestRepository.findWithUserAndSchoolById(requestId)
                .orElseThrow(() ->
                        new GeneralException(VerificationErrorStatus.VERIFICATION_REQUEST_NOT_FOUND)
                );

        // 승인/거절 완료된 요청은 이미지가 파기되어 있으므로 빈 문자열 반환
        String presignedUrl = req.getStatus() == VerificationStatus.PENDING
                ? fileStorageService.generateStudentCardPresignedUrl(req.getRequestImageUrl())
                : "";

        adminAuditLogService.record(
                adminUserId,
                AdminAuditAction.VIEW_VERIFICATION_REQUEST,
                AdminAuditTargetType.VERIFICATION_REQUEST,
                requestId,
                "학교 인증 요청 상세 열람",
                verificationMetadata(req)
        );

        return toDetail(req, presignedUrl);
    }

    // 학교 인증 요청 승인 처리
    public void approve(Long requestId, String adminComment) {
        Long adminUserId = getCurrentAdminUserId();

        UserSchoolVerificationRequest req = requestRepository.findWithUserAndSchoolById(requestId)
                .orElseThrow(() ->
                        new GeneralException(VerificationErrorStatus.VERIFICATION_REQUEST_NOT_FOUND)
                );

        validatePending(req);

        User user = req.getUser();

        // 이미 최종 인증된 사용자면 중복 승인 방지
        if (verificationRepository.existsByUserId(user.getId())) {
            throw new GeneralException(VerificationErrorStatus.SCHOOL_VERIFICATION_STATUS_INVALID);
        }

        // 요청 상태를 승인으로 변경
        req.approve(adminUserId, adminComment);

        // 승인 완료 후 S3에서 학생증 이미지 삭제
        fileStorageService.deleteStudentCardImage(req.getRequestImageUrl());

        // 최종 학교 인증 이력 저장
        verificationRepository.save(
                UserSchoolVerification.builder()
                        .verified_at(LocalDateTime.now())
                        .user(user)
                        .school(req.getSchool())
                        .build()
        );

        // 사용자 학교 인증 상태 반영
        user.verifySchool();

        adminAuditLogService.recordAfterCommit(
                adminUserId,
                AdminAuditAction.APPROVE_VERIFICATION_REQUEST,
                AdminAuditTargetType.VERIFICATION_REQUEST,
                requestId,
                adminComment,
                verificationMetadata(req)
        );
    }

    // 학교 인증 요청 거절 처리
    public void reject(Long requestId, String adminComment) {
        Long adminUserId = getCurrentAdminUserId();

        UserSchoolVerificationRequest req = requestRepository.findWithUserAndSchoolById(requestId)
                .orElseThrow(() ->
                        new GeneralException(VerificationErrorStatus.VERIFICATION_REQUEST_NOT_FOUND)
                );

        validatePending(req);

        // 요청 상태를 거절로 변경
        req.reject(adminUserId, adminComment);

        // 거절 후 S3에서 학생증 이미지 삭제
        fileStorageService.deleteStudentCardImage(req.getRequestImageUrl());

        adminAuditLogService.recordAfterCommit(
                adminUserId,
                AdminAuditAction.REJECT_VERIFICATION_REQUEST,
                AdminAuditTargetType.VERIFICATION_REQUEST,
                requestId,
                adminComment,
                verificationMetadata(req)
        );
    }

    // 처리 가능한 대기 상태 요청인지 검증
    private void validatePending(UserSchoolVerificationRequest req) {
        if (req.getStatus() != VerificationStatus.PENDING) {
            throw new GeneralException(VerificationErrorStatus.VERIFICATION_ALREADY_PROCESSED);
        }
    }

    private String verificationMetadata(UserSchoolVerificationRequest req) {
        return "userId=" + req.getUser().getId() + ",schoolId=" + req.getSchool().getId();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // 목록 응답 DTO 변환
    private VerificationAdminDTO.ListItemResponse toListItem(UserSchoolVerificationRequest req) {
        return VerificationAdminDTO.ListItemResponse.builder()
                .requestId(req.getId())
                .status(req.getStatus())
                .requestedAt(req.getRequestedAt())
                .userId(req.getUser().getId())
                .userName(req.getUser().getUsername()) // 또는 getName()
                .userEmail(req.getUser().getEmail())
                .schoolId(req.getSchool().getId())
                .schoolName(req.getSchool().getName())
                .build();
    }

    // 상세 응답 DTO 변환
    private VerificationAdminDTO.DetailResponse toDetail(UserSchoolVerificationRequest req, String presignedUrl) {
        return VerificationAdminDTO.DetailResponse.builder()
                .requestId(req.getId())
                .requestImageUrl(presignedUrl)
                .status(req.getStatus())
                .requestedAt(req.getRequestedAt())
                .userId(req.getUser().getId())
                .userName(req.getUser().getUsername()) // 또는 getName()
                .userEmail(req.getUser().getEmail())
                .schoolId(req.getSchool().getId())
                .schoolName(req.getSchool().getName())
                .processedAt(req.getProcessedAt())
                .processedBy(req.getProcessedBy())
                .adminComment(req.getAdminComment())
                .build();
    }

    // 현재 로그인한 관리자 userId 조회
    private Long getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User admin = (User) auth.getPrincipal();
        return admin.getId();
    }
}
