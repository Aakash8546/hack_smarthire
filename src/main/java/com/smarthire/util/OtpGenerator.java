package com.smarthire.util;

import java.security.SecureRandom;

public final class OtpGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private OtpGenerator() {
    }

    public static String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }
}
