package com.shu.backend.global;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailDebugRunner implements CommandLineRunner {

    private final JavaMailSenderImpl mailSender;

    @Override
    public void run(String... args) {
        Properties props = mailSender.getJavaMailProperties();

        log.debug("===== MAIL DEBUG START =====");
        log.debug("java.version={}", System.getProperty("java.version"));
        log.debug("java.vendor={}", System.getProperty("java.vendor"));
        log.debug("java.home={}", System.getProperty("java.home"));

        log.debug("mail.host={}", mailSender.getHost());
        log.debug("mail.port={}", mailSender.getPort());
        log.debug("mail.protocol={}", mailSender.getProtocol());
        log.debug("mail.username.empty={}",
                mailSender.getUsername() == null || mailSender.getUsername().isBlank());

        log.debug("mail.smtp.auth={}", props.getProperty("mail.smtp.auth"));
        log.debug("mail.smtp.starttls.enable={}", props.getProperty("mail.smtp.starttls.enable"));
        log.debug("mail.smtp.starttls.required={}", props.getProperty("mail.smtp.starttls.required"));
        log.debug("mail.smtp.ssl.enable={}", props.getProperty("mail.smtp.ssl.enable"));
        log.debug("mail.smtp.ssl.trust={}", props.getProperty("mail.smtp.ssl.trust"));

        log.debug("===== MAIL DEBUG END =====");
    }
}
