package com.shu.backend.domain.pushtoken.service;

import com.shu.backend.domain.pushtoken.dto.PushTokenDTO;
import com.shu.backend.domain.pushtoken.exception.PushTokenException;
import com.shu.backend.domain.pushtoken.exception.status.PushTokenErrorStatus;
import com.shu.backend.domain.pushtoken.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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

        return new PushTokenDTO.RegisterResponse(true);
    }

    @Transactional
    public int deactivateAll(Long userId) {
        return pushTokenRepository.deactivateAllByUserId(userId);
    }

    @Transactional
    public int deactivateByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new PushTokenException(PushTokenErrorStatus.INVALID_TOKEN);
        }
        return pushTokenRepository.deactivateByToken(token.trim());
    }
}
