package com.shu.backend.domain.push.service;

import com.google.firebase.messaging.*;
import com.shu.backend.domain.pushtoken.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushService {

    private final PushTokenRepository pushTokenRepository;

    public void sendToUserAfterCommit(Long userId, String title, String body, Map<String, String> data) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendToUser(userId, title, body, data);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendToUser(userId, title, body, data);
            }
        });
    }

    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        var tokens = pushTokenRepository.findByUserIdAndIsActiveTrue(userId);
        if (tokens.isEmpty()) {
            log.debug("Push skipped: userId={}, reason=no_active_token", userId);
            return;
        }

        List<String> tokenValues = tokens.stream().map(t -> t.getToken()).toList();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokenValues)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data == null ? Collections.emptyMap() : data)
                .build();

        try {
            BatchResponse resp = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            int failureCount = 0;

            // 실패한 토큰 비활성화
            for (int i = 0; i < resp.getResponses().size(); i++) {
                SendResponse r = resp.getResponses().get(i);
                if (!r.isSuccessful()) {
                    failureCount++;
                    String failedToken = tokenValues.get(i);
                    pushTokenRepository.deactivateByToken(failedToken);
                    log.warn("Push token deactivated: userId={}, tokenSuffix={}, error={}",
                            userId, tokenSuffix(failedToken), r.getException() != null ? r.getException().getMessage() : null);
                }
            }
            log.info("Push sent: userId={}, tokenCount={}, successCount={}, failureCount={}",
                    userId, tokenValues.size(), resp.getSuccessCount(), failureCount);
        } catch (Exception e) {
            log.warn("Push send failed: userId={}, tokenCount={}", userId, tokenValues.size(), e);
        }
    }

    private String tokenSuffix(String token) {
        if (token == null || token.length() <= 8) {
            return "unknown";
        }
        return token.substring(token.length() - 8);
    }
}
