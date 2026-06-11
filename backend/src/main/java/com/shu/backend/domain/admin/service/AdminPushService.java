package com.shu.backend.domain.admin.service;

import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.notification.service.NotificationService;
import com.shu.backend.domain.push.service.PushService;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 신고 접수·문의 접수·학교 인증 요청 등 관리자가 확인해야 하는 이벤트를
 * 활성(ACTIVE) 상태의 관리자 전원에게 알리는 서비스.
 *
 * FCM 푸시만 보내면 푸시를 놓쳤을 때 관리자가 이벤트를 영영 알 수 없으므로,
 * 알림함(Notification) 기록을 먼저 남기고 푸시를 함께 발송한다.
 * (프론트엔드 알림함 목록/미확인 카운트는 Notification 테이블 기준으로 동작한다)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPushService {

    private final UserRepository userRepository;
    private final PushService pushService;
    private final NotificationService notificationService;

    /**
     * 활성 관리자 전원에게 알림함 기록을 남기고 FCM 푸시를 발송한다.
     *
     * 관리자 알림 실패가 신고/문의/가입 등 원래 요청을 실패시키면 안 되므로
     * 관리자별로 예외를 잡아 로그만 남긴다.
     *
     * @param type       알림 타입 (ADMIN_REPORT / ADMIN_INQUIRY / ADMIN_VERIFICATION)
     * @param targetType 알림 탭 시 이동할 대상의 종류 (프론트 라우팅 분기에 사용)
     * @param targetId   대상 엔티티의 pk
     * @param title      푸시 제목 (알림함에는 message만 저장된다)
     * @param message    알림 문구 (알림함 메시지 + 푸시 본문)
     * @param actorId    이벤트를 일으킨 유저. 관리자 본인이 일으킨 이벤트는
     *                   자신에게 알리지 않기 위해 사용한다. (시스템 이벤트면 null)
     */
    public void notifyActiveAdmins(NotificationType type,
                                   NotificationTargetType targetType,
                                   Long targetId,
                                   String title,
                                   String message,
                                   Long actorId) {
        userRepository.findByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)
                .forEach(admin -> {
                    try {
                        // 알림함 기록 — actor가 관리자 본인이면 null이 반환되고 푸시도 생략한다
                        Long notificationId = notificationService.create(
                                type, targetType, targetId, message, admin.getId(), actorId);
                        if (notificationId == null) {
                            return;
                        }

                        // 푸시 data 페이로드 — 프론트엔드(fcm_service.dart)의 타입 분기 문자열과 일치해야 한다
                        pushService.sendToUserAfterCommit(
                                admin.getId(),
                                title,
                                message,
                                Map.of(
                                        "notificationId", String.valueOf(notificationId),
                                        "type", type.name(),
                                        "targetType", targetType.name(),
                                        "targetId", String.valueOf(targetId)
                                )
                        );
                    } catch (Exception e) {
                        log.warn("Admin notification failed: adminId={}, type={}, targetType={}, targetId={}",
                                admin.getId(), type, targetType, targetId, e);
                    }
                });
    }
}
