package com.smarthire.service.ml;

import java.util.List;

public final class MlDtos {

    private MlDtos() {
    }

    public record ResumeAnalysisResult(Double score, List<String> skills, String summary) {
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
