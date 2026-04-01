package com.smarthire.dto.candidate;

public record SubmitMockInterviewAnswerResponse(
        Long interviewId,
        int answeredQuestionNumber,
        Integer nextQuestionNumber,
        int totalQuestions,
        String nextQuestion,
        boolean completed,
        boolean analysisReady
) {
}
