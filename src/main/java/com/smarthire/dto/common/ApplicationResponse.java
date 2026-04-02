package com.smarthire.dto.common;

import java.util.List;
import java.time.OffsetDateTime;

public record ApplicationResponse(
        Long id,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        List<String> candidateSkills,
        Long candidateResumeId,
        Long jobId,
        String jobTitle,
        String status,
        String coverLetter,
        Long chatId,
        OffsetDateTime createdAt
) {
}
