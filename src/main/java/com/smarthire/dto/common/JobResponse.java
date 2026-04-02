package com.smarthire.dto.common;

import java.time.OffsetDateTime;
import java.util.List;

public record JobResponse(
        Long id,
        String title,
        String company,
        String location,
        String description,
        Integer experience,
        Integer minimumExperience,
        List<String> requiredSkills,
        String jobPackage,
        String status,
        Long recruiterId,
        String recruiterName,
        OffsetDateTime createdAt
) {
}
