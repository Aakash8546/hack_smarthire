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

    public record SkillGapResult(List<String> missingSkills, String roadmap) {
    }

    public record SpamDetectionResult(boolean spam) {
    }

    public record CheatingDetectionResult(String result) {
    }
}
