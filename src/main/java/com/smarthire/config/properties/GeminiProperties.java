package com.smarthire.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ai.gemini")
public record GeminiProperties(
        @NotBlank String baseUrl,
        @NotBlank String model,
        @NotBlank String apiKey,
        @NotBlank String apiKeyHeaderName
) {
}
