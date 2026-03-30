package com.smarthire.dto.recruiter;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateJobRequest(
        @NotBlank String title,
        @NotBlank String company,
        @NotBlank String location,
        @NotBlank @Size(max = 5000) String description,
        @NotNull @Min(0) Integer minimumExperience,
        @NotEmpty List<String> requiredSkills
) {
}
