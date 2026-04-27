package com.shu.backend.domain.auth.provider;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * 이메일 인증 메시지 발송을 담당하는 구현체.
 *
 * Thymeleaf 템플릿으로 HTML 이메일을 렌더링하여 발송한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationProvider implements VerificationMessageProvider {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${mail.from}")
    private String from;

    // 인증번호 HTML 이메일 발송
    @Override
    public void send(String email, String code) {
        try {
            Context context = new Context();
            context.setVariable("code", code);

            String htmlContent = templateEngine.process("email/verification-code", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("[Teenple] 이메일 인증번호 안내");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("이메일 발송 실패: target={}", email, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }
}
