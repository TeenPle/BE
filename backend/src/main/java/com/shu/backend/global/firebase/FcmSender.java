package com.shu.backend.global.firebase;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.shu.backend.domain.pushtoken.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmSender {

    private final PushTokenRepository pushTokenRepository;

    public void sendToUser(Long userId, String title, String body) {
        List<String> tokens = pushTokenRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(t -> t.getToken())
                .toList();

        for (String token : tokens) {
            try {
                Message message = Message.builder()
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setToken(token)
                        .build();
                FirebaseMessaging.getInstance().send(message);
            } catch (Exception e) {
                log.warn("FCM 전송 실패 (token={}): {}", token, e.getMessage());
            }
        }
    }
}
