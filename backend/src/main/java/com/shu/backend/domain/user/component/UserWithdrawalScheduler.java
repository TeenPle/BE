package com.shu.backend.domain.user.component;

import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탈퇴 유예 기간(7일) 만료 유저를 매일 새벽 3시에 영구 삭제 처리하는 스케줄러.
 * 유예 기간이 남아 있는 유저는 건드리지 않으며, 만료된 유저만 PII 익명화 후 DELETED 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawalScheduler {

    private static final int GRACE_PERIOD_DAYS = 7;

    private final UserRepository userRepository;
    private final UserService userService;

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredWithdrawals() {
        // 요청일로부터 7일이 지난 PENDING_DELETION 유저만 조회
        LocalDateTime threshold = LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS);
        List<Long> targetIds = userRepository.findExpiredPendingDeletionUserIds(
                UserStatus.PENDING_DELETION, threshold
        );

        if (targetIds.isEmpty()) {
            return;
        }

        log.info("[Withdrawal] 만료된 탈퇴 유예 계정 {}건 PII 파기 시작", targetIds.size());

        int success = 0;
        for (Long userId : targetIds) {
            try {
                userService.purgeExpiredAccount(userId);
                success++;
            } catch (Exception e) {
                // 단건 실패가 전체 배치를 멈추지 않도록 예외를 잡아 로그만 남긴다.
                log.error("[Withdrawal] userId={} PII 파기 실패: {}", userId, e.getMessage());
            }
        }

        log.info("[Withdrawal] 완료. {}/{} 건 처리됨", success, targetIds.size());
    }
}
