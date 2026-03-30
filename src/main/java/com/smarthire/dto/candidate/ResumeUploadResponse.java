package com.smarthire.dto.candidate;

import java.util.List;

public record ResumeUploadResponse(
        Long resumeId,
        String fileName,
        Double resumeScore,
        List<String> extractedSkills,
        String summary
) {
}
