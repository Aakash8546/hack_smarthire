package com.smarthire.service.impl;

import com.smarthire.config.properties.AppProperties;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.ApplicationStatus;
import com.smarthire.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Override
    @Async
    public void sendOtpEmail(User user, String otp) {
        sendEmail(user.getEmail(), "SmartHire OTP Verification", """
                Hello %s,

                Your SmartHire OTP is: %s
                It will expire in 10 minutes.

                If you did not request this, please ignore this email.
                """.formatted(user.getName(), otp));
    }

    @Override
    @Async
    public void sendJobPostedEmail(String candidateEmail, String candidateName, String jobTitle, String companyName,
                                   String jobPackage, String location) {
        sendEmail(candidateEmail, "New job posted on SmartHire", """
                Hello %s,

                A new job has been posted on SmartHire.
                Title: %s
                Company: %s
                Package: %s
                Location: %s

                Log in to SmartHire to view and apply.
                """.formatted(candidateName, jobTitle, companyName, jobPackage, location));
    }

    @Override
    @Async
    public void sendApplicationStatusUpdatedEmail(String candidateEmail, String candidateName, String jobTitle,
                                                  String companyName, ApplicationStatus status, String optionalMessage) {
        String messageSection = optionalMessage == null || optionalMessage.isBlank()
                ? ""
                : "\nMessage: " + optionalMessage.trim() + "\n";
        sendEmail(candidateEmail, "Application status updated", """
                Hello %s,

                Your application status has been updated.
                Job Title: %s
                Company: %s
                Status: %s%s

                Please check SmartHire for more details.
                """.formatted(
                candidateName,
                jobTitle,
                companyName,
                status.name(),
                messageSection
        ));
    }

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.mail().from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.warn("Email sending failed for {}. Subject: {}. Cause: {}", to, subject, exception.getMessage());
        }
    }
}
