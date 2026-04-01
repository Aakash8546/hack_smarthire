package com.smarthire.service.ai;

import java.util.List;
import java.util.Map;

import com.smarthire.dto.candidate.MockInterviewQuestionResponse;

public interface GeminiInterviewQuestionService {

    MockInterviewQuestionResponse generateQuestions(List<String> skills);

    Map<String, Object> analyzeInterview(List<String> skills, List<Map<String, Object>> questionAnswers);
}
