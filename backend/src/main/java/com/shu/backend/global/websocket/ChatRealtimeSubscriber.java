package com.shu.backend.global.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRealtimeSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRealtimePublisher publisher;

    public void onMessage(String rawMessage) {
        try {
            ChatRealtimeMessage message = objectMapper.readValue(rawMessage, ChatRealtimeMessage.class);
            if (publisher.getOriginId().equals(message.originId())) {
                return;
            }
            messagingTemplate.convertAndSend(message.destination(), message.payload());
        } catch (Exception e) {
            log.warn("Failed to relay chat realtime message. rawMessage={}", rawMessage, e);
        }
    }
}
