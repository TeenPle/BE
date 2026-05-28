package com.shu.backend.domain.pushtoken.service;

import com.shu.backend.domain.pushtoken.dto.PushTokenDTO;
import com.shu.backend.domain.pushtoken.exception.PushTokenException;
import com.shu.backend.domain.pushtoken.exception.status.PushTokenErrorStatus;
import com.shu.backend.domain.pushtoken.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;

    @Transactional
    public PushTokenDTO.RegisterResponse registerOrUpdate(Long userId, PushTokenDTO.RegisterRequest req) {

        String token = (req.getToken() == null) ? null : req.getToken().trim();
        if (token == null || token.isEmpty()) {
            throw new PushTokenException(PushTokenErrorStatus.INVALID_TOKEN);
        }

        // DB 레벨 upsert: 없으면 INSERT, 있으면 userId/platform/is_active 갱신
        // 동시 요청으로 인한 race condition이 발생하지 않음
        pushTokenRepository.upsert(userId, token, req.getPlatform().name());
        log.info("Push token registered: userId={}, platform={}, tokenSuffix={}",
                userId, req.getPlatform(), tokenSuffix(token));

        return new PushTokenDTO.RegisterResponse(true);
    }

    @Transactional
    public int deactivateAll(Long userId) {
        int count = pushTokenRepository.deactivateAllByUserId(userId);
        log.info("Push tokens deactivated: userId={}, count={}", userId, count);
        return count;
    }

    @Transactional
    public int deactivateByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new PushTokenException(PushTokenErrorStatus.INVALID_TOKEN);
        }
        String normalizedToken = token.trim();
        int count = pushTokenRepository.deactivateByToken(normalizedToken);
        log.info("Push token deactivated: tokenSuffix={}, count={}", tokenSuffix(normalizedToken), count);
        return count;
    }

    private String tokenSuffix(String token) {
        if (token == null || token.length() <= 8) {
            return "unknown";
        }
        return token.substring(token.length() - 8);
    }
}
