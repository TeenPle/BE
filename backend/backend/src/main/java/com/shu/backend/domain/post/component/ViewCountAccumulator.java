package com.shu.backend.domain.post.component;


import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

@Component
public class ViewCountAccumulator {

    private final Cache<Long, LongAdder> nativeCache;

    @SuppressWarnings("unchecked")
    public ViewCountAccumulator(CacheManager cacheManager) {
        CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache("postViewCounter");
        if (caffeineCache == null) throw new IllegalStateException("postViewCounter missing");

        // 핵심: Cache<Object,Object> -> Cache<?,?> -> Cache<Long,LongAdder>
        this.nativeCache = (Cache<Long, LongAdder>) (Cache<?, ?>) caffeineCache.getNativeCache();
    }

    public void increment(Long postId) {
        nativeCache.asMap().computeIfAbsent(postId, k -> new LongAdder()).increment();
    }

    public Map<Long, LongAdder> snapshot() {
        return nativeCache.asMap();
    }
}