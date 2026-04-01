package com.smarthire.dto.candidate;

import java.util.List;

public record MockInterviewQuestionResponse(
        List<QuestionItem> questions
) {

    public record QuestionItem(
            int questionNumber,
            String question
    ) {
    }
}
