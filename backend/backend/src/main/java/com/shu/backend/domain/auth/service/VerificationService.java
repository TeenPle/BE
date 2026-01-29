package com.shu.backend.domain.auth.service;


import com.shu.backend.domain.auth.provider.VerificationMessageProvider;
import com.shu.backend.domain.auth.store.VerificationCodeStore;
import com.shu.backend.domain.auth.store.VerificationTokenStore;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationMessageProvider provider;
    private final VerificationCodeStore codeStore;
    private final VerificationTokenStore tokenStore;


    /**
     * 이메일로 코드를 발생하는 함수
     */
    public String sendCode(String target) {
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        codeStore.save(target, code);
        provider.send(
                target,
                "[Teenple] 이메일 인증번호 안내\n\n"
                        + "안녕하세요, Teenple입니다.\n"
                        + "회원가입을 위한 인증번호는 아래와 같습니다.\n\n"
                        + code + "\n\n"
                        + "인증번호는 5분간 유효합니다.\n"
                        + "요청하지 않은 경우 본 메일을 무시해주세요."
        );
        return code;
    }

    /**
     *  Redis에 저장되어있는 인증코드와 맞는지 확인
     */
    public String verifyCode(String target, String code) {
        String saved = codeStore.get(target);
        if (saved == null || !saved.equals(code)) {
            throw new UserException(UserErrorStatus.VERIFICATION_CODE_INVALID);
        }

        codeStore.delete(target);

        String token = "verif_" + UUID.randomUUID();
        tokenStore.save(token, target);

        return token;
    }

    /**
     * 인증을 거치지 않을경우 예외를 발생
     */
    public void verifyTokenOrThrow(String token, String target) {
        String saved = tokenStore.get(token);
        //토큰 자체가 없다면
        if (saved == null) {
            throw new UserException(UserErrorStatus.VERIFICATION_TOKEN_INVALID_OR_EXPIRED);
        }
        //인증이 아닐경우
        if (!saved.equals(target)) {
            throw new UserException(UserErrorStatus.VERIFICATION_TARGET_MISMATCH);
        }

        tokenStore.consume(token);
    }
}