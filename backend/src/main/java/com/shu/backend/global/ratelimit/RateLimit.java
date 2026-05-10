package com.shu.backend.global.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 슬라이딩 윈도우 방식의 Rate Limiting 애노테이션.
 * byIp=false(기본): 인증된 사용자(userId) 단위
 * byIp=true: 클라이언트 IP 단위 (비인증 엔드포인트용)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** Redis 키 구분자 (예: "post", "comment", "login") */
    String key();

    /** 허용 최대 횟수 */
    int limit();

    /** 슬라이딩 윈도우 크기 (초) */
    int windowSeconds();

    /** true이면 클라이언트 IP 기준으로 제한 (비인증 엔드포인트용) */
    boolean byIp() default false;
}
