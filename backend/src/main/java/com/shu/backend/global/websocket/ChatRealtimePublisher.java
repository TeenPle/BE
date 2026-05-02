package com.shu.backend.global.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRealtimePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private final String originId = UUID.randomUUID().toString();

    @Value("${chat.realtime.redis-channel:chat:realtime}")
    private String channel;

    public void publish(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);

        try {
            ChatRealtimeMessage message = new ChatRealtimeMessage(
                    originId,
                    destination,
                    objectMapper.valueToTree(payload)
            );
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize chat realtime message. destination={}", destination, e);
        } catch (Exception e) {
            log.warn("Failed to publish chat realtime message to Redis. destination={}", destination, e);
        }
    }

    public String getOriginId() {
        return originId;
    }
}
