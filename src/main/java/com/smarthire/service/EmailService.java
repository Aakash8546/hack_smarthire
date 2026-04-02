package com.smarthire.service;

import com.smarthire.entity.enums.ApplicationStatus;

public interface EmailService {

    void sendOtpEmail(com.smarthire.entity.User user, String otp);

    void sendJobPostedEmail(String candidateEmail, String candidateName, String jobTitle, String companyName, String jobPackage, String location);

    void sendApplicationStatusUpdatedEmail(String candidateEmail, String candidateName, String jobTitle, String companyName,
                                           ApplicationStatus status, String optionalMessage);
}
