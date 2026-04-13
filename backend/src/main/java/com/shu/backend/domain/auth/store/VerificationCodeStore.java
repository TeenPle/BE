package com.shu.backend.domain.auth.store;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 휴대폰 인증번호를 Redis에 임시 저장/조회/삭제하는 저장소.
 *
 * 문자로 발송한 인증번호를 짧은 시간 동안 보관하고,
 * 사용자가 입력한 인증번호와 비교할 때 사용한다.
 */
@Component
public class VerificationCodeStore {

    private final StringRedisTemplate redisTemplate;

    public VerificationCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String phone) {
        return "sms:code:" + phone;
    }

    // 이메일 인증번호 저장
    public void save(String phone, String code) {
        redisTemplate.opsForValue().set(key(phone), code, 3, TimeUnit.MINUTES);
    }

    // 저장된 인증번호 조회
    public String get(String phone) {
        return redisTemplate.opsForValue().get(key(phone));
    }

    // 인증 완료 또는 만료 처리 시 인증번호 삭제
    public void delete(String phone) {
        redisTemplate.delete(key(phone));
    }
}