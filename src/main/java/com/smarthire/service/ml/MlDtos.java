package com.smarthire.service.ml;

import java.util.List;
import java.util.Map;

public final class MlDtos {

    private MlDtos() {
    }

    public record ResumeAnalysisResult(
            String analyzedFileName,
            Double score,
            List<String> skills,
            String summary,
            Double experienceYears,
            Map<String, Object> results,
            Map<String, Object> rawResponse
    ) {
    }

    public record RecommendationResult(List<Long> recommendedJobIds, String reason) {
    }

    public record MockInterviewResult(String technicalAnalysis, String behavioralAnalysis) {
    }

    public record SkillGapResult(List<String> missingSkills, String roadmap, List<SkillLearningResource> learningResources) {
    }

    public record SkillLearningResource(
            String skill,
            String searchQuery,
            List<LearningVideo> videos
    ) {
    }

    public record LearningVideo(
            String title,
            String url,
            String channel,
            String duration,
            String views,
            String thumbnail
    ) {
    }

    public record SpamDetectionResult(boolean spam) {
    }

    public record CheatingDetectionResult(String result) {
    }
}
