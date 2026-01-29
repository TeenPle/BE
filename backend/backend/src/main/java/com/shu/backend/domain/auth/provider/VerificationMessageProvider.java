package com.shu.backend.domain.auth.provider;

public interface VerificationMessageProvider {
    void send(String target, String message);
}