package com.smarthire.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ml.api")
public record MlApiProperties(
        @NotBlank String baseUrl,
        @NotBlank String resumeAnalyzePath,
        @NotBlank String skillVideosBaseUrl,
        @NotBlank String skillVideosPath,
        @NotBlank String spamBaseUrl,
        @NotBlank String spamPredictPath,
        @Min(1000) int timeoutMillis,
        boolean enabled,
        String apiKey,
        String apiKeyHeaderName
) {
}
