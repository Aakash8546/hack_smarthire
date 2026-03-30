package com.smarthire.dto.common;

import java.time.OffsetDateTime;

import com.smarthire.entity.enums.ApplicationStatus;

public record ApplicationResponse(
        Long id,
        Long candidateId,
        String candidateName,
        Long jobId,
        String jobTitle,
        ApplicationStatus status,
        String coverLetter,
        Long chatId,
        OffsetDateTime createdAt
) {
}
