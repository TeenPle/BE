package com.shu.backend.global.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("postViewCounter");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100_000)                 // 예상 postId 수에 맞춰 조정
                .expireAfterAccess(Duration.ofMinutes(10)) // 핫키만 남기고 자연 소멸
        );
        return manager;
    }
}
