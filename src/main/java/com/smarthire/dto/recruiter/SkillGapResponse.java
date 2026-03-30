package com.smarthire.dto.recruiter;

import java.time.OffsetDateTime;
import java.util.List;

public record SkillGapResponse(
        Long analysisId,
        Long jobId,
        String jobTitle,
        List<String> missingSkills,
        String roadmap,
        OffsetDateTime createdAt
) {
}
