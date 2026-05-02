package com.shu.backend.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class ChatRealtimeRedisConfig {

    private final ChatRealtimeSubscriber subscriber;

    @Value("${chat.realtime.redis-channel:chat:realtime}")
    private String channel;

    @Bean
    public RedisMessageListenerContainer chatRealtimeRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                (message, pattern) -> subscriber.onMessage(
                        new String(message.getBody(), StandardCharsets.UTF_8)
                ),
                new ChannelTopic(channel)
        );
        return container;
    }
}
