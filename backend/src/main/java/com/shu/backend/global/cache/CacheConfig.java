package com.shu.backend.global.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                // 게시글 조회수 누산 (10분 TTL)
                new CaffeineCache("postViewCounter",
                        Caffeine.newBuilder()
                                .maximumSize(100_000)
                                .expireAfterAccess(Duration.ofMinutes(10))
                                .build()),
                // 급식 데이터 (6시간 TTL — 하루 1~2회 변경)
                new CaffeineCache("meal",
                        Caffeine.newBuilder()
                                .maximumSize(1_000)
                                .expireAfterWrite(Duration.ofHours(6))
                                .build()),
                // 시간표 데이터 (24시간 TTL — 학기 중 거의 변경 없음)
                new CaffeineCache("timetable",
                        Caffeine.newBuilder()
                                .maximumSize(5_000)
                                .expireAfterWrite(Duration.ofHours(24))
                                .build())
        ));
        return manager;
    }
}
