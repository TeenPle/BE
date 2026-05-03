package com.shu.backend.global.ratelimit;

import com.shu.backend.global.apiPayload.code.status.ErrorStatus;
import com.shu.backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Before("@annotation(rateLimit)")
    public void check(RateLimit rateLimit) {
        String userId = resolveUserId();
        if (userId == null) return;

        String redisKey = "rate:" + rateLimit.key() + ":" + userId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (long) rateLimit.windowSeconds() * 1000;

        try {
            // 슬라이딩 윈도우: 오래된 항목 제거 → 현재 개수 확인 → 새 항목 추가
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, Double.NEGATIVE_INFINITY, windowStart);

            Long count = redisTemplate.opsForZSet().zCard(redisKey);
            if (count != null && count >= rateLimit.limit()) {
                throw new GeneralException(ErrorStatus.RATE_LIMIT_EXCEEDED);
            }

            // member에 UUID를 포함해 동일 ms 내 복수 요청도 모두 별개로 기록
            redisTemplate.opsForZSet().add(redisKey, now + ":" + UUID.randomUUID(), now);
            // TTL을 윈도우 크기 + 여유분으로 설정해 미사용 키 자동 삭제
            redisTemplate.expire(redisKey,
                    java.time.Duration.ofSeconds(rateLimit.windowSeconds() + 10));
        } catch (GeneralException e) {
            throw e; // 실제 rate limit 초과는 그대로 전파
        } catch (Exception e) {
            log.warn("RateLimit Redis 연결 실패, 제한 없이 통과합니다. key={}", redisKey, e);
        }
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof com.shu.backend.domain.user.entity.User user) {
            return String.valueOf(user.getId());
        }
        if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return null;
    }
}
