package com.shu.backend.domain.push.service;

import com.google.firebase.messaging.*;
import com.shu.backend.domain.pushtoken.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushService {

    private final PushTokenRepository pushTokenRepository;
    private final PlatformTransactionManager transactionManager;

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
                    deactivateTokenInNewTransaction(failedToken);
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

    // afterCommit 콜백에서는 기존 트랜잭션이 이미 커밋된 상태이므로 새 트랜잭션으로 실행해야 한다
    private void deactivateTokenInNewTransaction(String token) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> pushTokenRepository.deactivateByToken(token));
    }

    private String tokenSuffix(String token) {
        if (token == null || token.length() <= 8) {
            return "unknown";
        }
        return token.substring(token.length() - 8);
    }
}
