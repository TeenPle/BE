package com.shu.backend.domain.auth.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 이메일 인증 메시지 발송을 담당하는 구현체.
 *
 * VerificationMessageProvider 인터페이스를 구현하며,
 * 전달받은 인증 메시지를 이메일 형태로 사용자에게 전송한다.
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationProvider implements VerificationMessageProvider {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    // 이메일 인증 메시지 전송
    @Override
    public void send(String email, String message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(email);
        mail.setSubject("[Teenple] 이메일 인증");
        mail.setText(message);

        mailSender.send(mail);
    }
}