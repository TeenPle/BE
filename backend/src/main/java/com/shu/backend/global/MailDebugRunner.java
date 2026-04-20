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

        log.info("===== MAIL DEBUG START =====");
        log.info("java.version={}", System.getProperty("java.version"));
        log.info("java.vendor={}", System.getProperty("java.vendor"));
        log.info("java.home={}", System.getProperty("java.home"));

        log.info("mail.host={}", mailSender.getHost());
        log.info("mail.port={}", mailSender.getPort());
        log.info("mail.protocol={}", mailSender.getProtocol());
        log.info("mail.username.empty={}",
                mailSender.getUsername() == null || mailSender.getUsername().isBlank());

        log.info("mail.smtp.auth={}", props.getProperty("mail.smtp.auth"));
        log.info("mail.smtp.starttls.enable={}", props.getProperty("mail.smtp.starttls.enable"));
        log.info("mail.smtp.starttls.required={}", props.getProperty("mail.smtp.starttls.required"));
        log.info("mail.smtp.ssl.enable={}", props.getProperty("mail.smtp.ssl.enable"));
        log.info("mail.smtp.ssl.trust={}", props.getProperty("mail.smtp.ssl.trust"));

        log.info("===== MAIL DEBUG END =====");
    }
}