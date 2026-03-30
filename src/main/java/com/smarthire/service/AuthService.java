package com.smarthire.service;

import com.smarthire.dto.auth.AuthResponse;
import com.smarthire.dto.auth.CurrentUserResponse;
import com.smarthire.dto.auth.LoginRequest;
import com.smarthire.dto.auth.ResendOtpRequest;
import com.smarthire.dto.auth.SignupRequest;
import com.smarthire.dto.auth.VerifyOtpRequest;

public interface AuthService {

    String signup(SignupRequest request);

    String verifyOtp(VerifyOtpRequest request);

    String resendOtp(ResendOtpRequest request, String apiKey);

    AuthResponse login(LoginRequest request);

    CurrentUserResponse getCurrentUser();
}
