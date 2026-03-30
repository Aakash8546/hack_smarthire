package com.smarthire.service.impl;

import com.smarthire.config.properties.AppProperties;
import com.smarthire.entity.User;
import com.smarthire.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Override
    public void sendOtpEmail(User user, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.mail().from());
        message.setTo(user.getEmail());
        message.setSubject("SmartHire OTP Verification");
        message.setText("""
                Hello %s,

                Your SmartHire OTP is: %s
                It will expire in 10 minutes.

                If you did not request this, please ignore this email.
                """.formatted(user.getName(), otp));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.warn("Email sending failed for {}. OTP: {}. Cause: {}", user.getEmail(), otp, exception.getMessage());
        }
    }
}
