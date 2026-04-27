package com.shu.backend.domain.auth.provider;

public interface VerificationMessageProvider {
    void send(String target, String code);

    default void sendPasswordReset(String target, String code) {
        send(target, code);
    }
}
