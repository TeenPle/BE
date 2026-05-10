package com.shu.backend.global.ratelimit;

import com.shu.backend.global.apiPayload.code.status.ErrorStatus;
import com.shu.backend.global.exception.GeneralException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
        String subject;
        if (rateLimit.byIp()) {
            subject = resolveClientIp();
            if (subject == null) return;
        } else {
            subject = resolveUserId();
            if (subject == null) return;
        }

        String redisKey = "rate:" + rateLimit.key() + ":" + subject;
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
            throw e;
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

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            return getClientIp(request);
        } catch (Exception e) {
            log.warn("RateLimit: 요청 IP를 가져올 수 없습니다.", e);
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // 프록시 체인의 첫 번째(실제 클라이언트) IP만 사용
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
