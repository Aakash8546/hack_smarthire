package com.smarthire.dto.recruiter;

import java.time.OffsetDateTime;
import java.util.List;

public record SkillGapResponse(
        Long analysisId,
        Long jobId,
        String jobTitle,
        List<String> missingSkills,
        String roadmap,
        List<LearningResource> learningResources,
        OffsetDateTime createdAt
) {

    public record LearningResource(
            String skill,
            String searchQuery,
            List<VideoResource> videos
    ) {
    }

    public record VideoResource(
            String title,
            String url,
            String channel,
            String duration,
            String views,
            String thumbnail
    ) {
    }
}
