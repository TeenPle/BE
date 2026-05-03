package com.shu.backend.global.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 슬라이딩 윈도우 방식의 Rate Limiting 애노테이션.
 * 인증된 사용자(userId) 단위로 windowSeconds 시간 내 limit 횟수를 초과하면 429 응답.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** Redis 키 구분자 (예: "post", "comment", "report") */
    String key();

    /** 허용 최대 횟수 */
    int limit();

    /** 슬라이딩 윈도우 크기 (초) */
    int windowSeconds();
}
