package com.smarthire.service.impl;

import java.time.OffsetDateTime;

import com.smarthire.dto.auth.AuthResponse;
import com.smarthire.dto.auth.CurrentUserResponse;
import com.smarthire.dto.auth.LoginRequest;
import com.smarthire.dto.auth.SignupRequest;
import com.smarthire.dto.auth.VerifyOtpRequest;
import com.smarthire.entity.User;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.exception.UnauthorizedException;
import com.smarthire.repository.UserRepository;
import com.smarthire.security.JwtService;
import com.smarthire.security.SecurityUser;
import com.smarthire.service.AuthService;
import com.smarthire.service.EmailService;
import com.smarthire.util.OtpGenerator;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public String signup(SignupRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email is already registered");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.userType());
        user.setVerified(false);
        String otp = OtpGenerator.generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiry(OffsetDateTime.now().plusMinutes(10));
        userRepository.save(user);
        emailService.sendOtpEmail(user, otp);
        return "Signup successful. Please verify the OTP sent to your email.";
    }

    @Override
    @Transactional
    public String verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.isVerified()) {
            return "User is already verified.";
        }
        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.otp())) {
            throw new BadRequestException("Invalid OTP");
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("OTP has expired");
        }
        user.setVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        return "OTP verified successfully.";
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isVerified()) {
            throw new UnauthorizedException("Please verify your account before logging in");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        SecurityUser securityUser = new SecurityUser(user);
        return new AuthResponse(jwtService.generateToken(securityUser), user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    @Override
    public CurrentUserResponse getCurrentUser() {
        SecurityUser securityUser = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(securityUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new CurrentUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isVerified(), user.getSkills());
    }
}
