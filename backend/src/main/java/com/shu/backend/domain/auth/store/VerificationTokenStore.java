package com.shu.backend.domain.auth.store;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 휴대폰 인증 완료 후 발급되는 검증 토큰을 Redis에 저장/조회/삭제하는 저장소.
 *
 * 인증번호 검증이 끝난 사용자가 이후 회원가입 요청에서도
 * 인증 완료 상태를 증명할 수 있도록 임시 토큰을 관리한다.
 */
@Component
public class VerificationTokenStore {

    private final StringRedisTemplate redisTemplate;

    public VerificationTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String token) {
        return "sms:verify:" + token;
    }

    // 인증 완료 토큰 저장
    public void save(String token, String phone) {
        redisTemplate.opsForValue().set(key(token), phone, 5, TimeUnit.MINUTES);
    }

    // 토큰으로 인증된 휴대폰 번호 조회
    public String get(String token) {
        return redisTemplate.opsForValue().get(key(token));
    }

    // 토큰 1회 사용 후 제거
    public void consume(String token) {
        redisTemplate.delete(key(token));
    }
}