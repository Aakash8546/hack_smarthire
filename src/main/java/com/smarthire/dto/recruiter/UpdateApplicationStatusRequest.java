package com.smarthire.dto.recruiter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateApplicationStatusRequest(
        @NotBlank String status,
        @Size(max = 1000) String message
) {
}
