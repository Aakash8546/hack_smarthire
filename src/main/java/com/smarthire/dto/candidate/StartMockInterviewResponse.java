package com.smarthire.dto.candidate;

public record StartMockInterviewResponse(
        Long interviewId,
        int currentQuestionNumber,
        int totalQuestions,
        String question,
        String sessionStatus,
        boolean resumedExistingSession,
        boolean completed,
        boolean analysisReady
) {
}
