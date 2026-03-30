package com.smarthire.dto.candidate;

import java.time.OffsetDateTime;

public record MockInterviewResponse(
        Long interviewId,
        String technicalAnalysis,
        String behavioralAnalysis,
        OffsetDateTime createdAt
) {
}
