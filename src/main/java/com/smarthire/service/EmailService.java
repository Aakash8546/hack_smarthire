package com.smarthire.service;

import com.smarthire.entity.User;

public interface EmailService {

    void sendOtpEmail(User user, String otp);
}
