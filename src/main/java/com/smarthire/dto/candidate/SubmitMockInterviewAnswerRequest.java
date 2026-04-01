package com.smarthire.dto.candidate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SubmitMockInterviewAnswerRequest(
        @NotNull
        @Positive
        Integer questionNumber,
        @NotBlank
        @Size(max = 5000)
        String answer
) {
}
