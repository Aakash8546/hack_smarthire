package com.smarthire.dto.candidate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record MockInterviewResultResponse(
        Long interviewId,
        String sessionStatus,
        boolean completed,
        Integer score,
        String feedback,
        String technicalAnalysis,
        String behavioralAnalysis,
        List<String> strengths,
        List<String> weaknesses,
        Map<String, Object> analysis,
        List<QuestionAnswerItem> questionAnswers,
        OffsetDateTime updatedAt
) {

    public record QuestionAnswerItem(
            int questionNumber,
            String question,
            String answer
    ) {
    }
}
