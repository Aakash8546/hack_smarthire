package com.smarthire.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        File file,
        Mail mail,
        Frontend frontend,
        Video video,
        Auth auth
) {

    public record Jwt(@NotBlank String secret, long accessTokenExpiration) {
    }

    public record File(@NotBlank String uploadDir) {
    }

    public record Mail(@NotBlank String from) {
    }

    public record Frontend(@NotBlank String baseUrl) {
    }

    public record Video(@NotBlank String baseUrl, @NotBlank String provider) {
    }

    public record Auth(@NotBlank String resendOtpApiKey) {
    }
}
