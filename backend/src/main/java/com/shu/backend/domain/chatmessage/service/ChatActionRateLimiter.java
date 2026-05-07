package com.shu.backend.domain.chatmessage.service;

import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatActionRateLimiter {

    private final StringRedisTemplate redisTemplate;

    public void check(Long userId, String action, int limit, int windowSeconds) {
        if (userId == null) {
            throw new ChatMessageException(ChatMessageErrorStatus.CHAT_RATE_LIMITED);
        }

        String redisKey = "rate:chat:" + action + ":" + userId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (long) windowSeconds * 1000;

        try {
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, Double.NEGATIVE_INFINITY, windowStart);

            Long count = redisTemplate.opsForZSet().zCard(redisKey);
            if (count != null && count >= limit) {
                throw new ChatMessageException(ChatMessageErrorStatus.CHAT_RATE_LIMITED);
            }

            redisTemplate.opsForZSet().add(redisKey, now + ":" + UUID.randomUUID(), now);
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds + 10L));
        } catch (ChatMessageException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Chat rate limit Redis 연결 실패, 제한 없이 통과합니다. key={}", redisKey, e);
        }
    }
}
