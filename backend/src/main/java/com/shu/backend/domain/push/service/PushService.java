package com.shu.backend.domain.push.service;

import com.google.firebase.messaging.*;
import com.shu.backend.domain.pushtoken.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PushService {

    private final PushTokenRepository pushTokenRepository;

    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        var tokens = pushTokenRepository.findByUserIdAndIsActiveTrue(userId);
        if (tokens.isEmpty()) return;

        List<String> tokenValues = tokens.stream().map(t -> t.getToken()).toList();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokenValues)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data == null ? Collections.emptyMap() : data)
                .build();

        try {
            BatchResponse resp = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            // 실패한 토큰 비활성화
            for (int i = 0; i < resp.getResponses().size(); i++) {
                SendResponse r = resp.getResponses().get(i);
                if (!r.isSuccessful()) {
                    String failedToken = tokenValues.get(i);
                    System.out.println("[PUSH] 토큰 전송 실패, 비활성화: " + failedToken + " / error: " + r.getException());
                    pushTokenRepository.deactivateByToken(failedToken);
                } else {
                    System.out.println("[PUSH] 전송 성공: userId=" + userId);
                }
            }
        } catch (Exception e) {
            System.out.println("[PUSH ERROR] FCM 전송 예외: " + e.getMessage());
        }
    }
}
