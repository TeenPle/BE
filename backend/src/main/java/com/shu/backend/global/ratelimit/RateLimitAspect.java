package com.shu.backend.global.ratelimit;

import com.shu.backend.global.apiPayload.code.status.ErrorStatus;
import com.shu.backend.global.exception.GeneralException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object check(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String subject = rateLimit.byIp() ? resolveClientIp() : resolveUserId();
        if (subject == null) {
            return joinPoint.proceed();
        }

        String redisKey = "rate:" + rateLimit.key() + ":" + subject;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (long) rateLimit.windowSeconds() * 1000;

        try {
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, Double.NEGATIVE_INFINITY, windowStart);
            Long count = redisTemplate.opsForZSet().zCard(redisKey);
            if (count != null && count >= rateLimit.limit()) {
                throw new GeneralException(ErrorStatus.RATE_LIMIT_EXCEEDED);
            }
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.warn("RateLimit Redis 연결 실패, 제한 없이 통과합니다. key={}", redisKey, e);
            return joinPoint.proceed();
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            if (rateLimit.countFailures()) {
                safeRecord(redisKey, now, rateLimit.windowSeconds());
            }
            throw e;
        }

        safeRecord(redisKey, now, rateLimit.windowSeconds());
        return result;
    }

    private void safeRecord(String redisKey, long now, int windowSeconds) {
        try {
            redisTemplate.opsForZSet().add(redisKey, now + ":" + UUID.randomUUID(), now);
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds + 10L));
        } catch (Exception e) {
            log.warn("RateLimit Redis 기록 실패. 요청 결과는 유지합니다. key={}", redisKey, e);
        }
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof com.shu.backend.domain.user.entity.User user) {
            return String.valueOf(user.getId());
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
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
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String addr) {
        if (addr == null) return false;
        if (addr.equals("127.0.0.1") || addr.equals("::1") || addr.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        if (addr.startsWith("10.") || addr.startsWith("192.168.")) {
            return true;
        }
        if (addr.startsWith("172.")) {
            try {
                int second = Integer.parseInt(addr.split("\\.")[1]);
                return second >= 16 && second <= 31;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
