package com.smarthire.dto.candidate;

public record ResumeExistsResponse(
        Long candidateId,
        boolean uploaded,
        Long resumeId,
        String fileName
) {
}
