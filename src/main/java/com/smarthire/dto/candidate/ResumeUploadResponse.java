package com.smarthire.dto.candidate;

import java.util.List;
import java.util.Map;

public record ResumeUploadResponse(
        Long resumeId,
        String storedFileName,
        String originalFileName,
        Double resumeScore,
        List<String> extractedSkills,
        Double experienceYears,
        Map<String, Object> mlResults,
        String summary
) {
}
