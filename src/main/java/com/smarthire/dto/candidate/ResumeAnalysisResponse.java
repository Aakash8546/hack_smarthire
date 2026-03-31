package com.smarthire.dto.candidate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ResumeAnalysisResponse(
        Long resumeId,
        Long candidateId,
        String storedFileName,
        String originalFileName,
        Double resumeScore,
        List<String> extractedSkills,
        Double experienceYears,
        Map<String, Object> mlResults,
        Map<String, Object> rawResponse,
        String summary,
        OffsetDateTime updatedAt
) {
}
