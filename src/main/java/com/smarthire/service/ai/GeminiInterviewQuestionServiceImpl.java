package com.smarthire.service.ai;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.smarthire.config.properties.GeminiProperties;
import com.smarthire.dto.candidate.MockInterviewQuestionResponse;
import com.smarthire.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiInterviewQuestionServiceImpl implements GeminiInterviewQuestionService {

    private final RestTemplate mlRestTemplate;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    @Override
    public MockInterviewQuestionResponse generateQuestions(List<String> skills) {
        try {
            Map<String, Object> response = callGemini(buildQuestionPrompt(skills), 1200, "text/plain", null);
            log.info("Raw Gemini mock interview response: {}", response);
            String text = extractGeneratedText(response);
            if (text == null || text.isBlank()) {
                throw new BadRequestException("Gemini returned an empty response for mock interview questions");
            }
            MockInterviewQuestionResponse parsed = ensureTenQuestions(parseQuestionsFromText(text), skills);
            validateQuestions(parsed);
            return parsed;
        } catch (Exception exception) {
            throw new BadRequestException("Failed to generate mock interview questions using Gemini: " + exception.getMessage());
        }
    }

    @Override
    public Map<String, Object> analyzeInterview(List<String> skills, List<Map<String, Object>> questionAnswers) {
        try {
            Map<String, Object> response = callGemini(buildAnalysisPrompt(skills, questionAnswers), 1600, "application/json", buildAnalysisResponseSchema());
            log.info("Raw Gemini mock interview analysis response: {}", response);
            String jsonText = normalizeJsonText(extractGeneratedText(response));
            if (jsonText == null || jsonText.isBlank()) {
                throw new BadRequestException("Gemini returned an empty response for mock interview analysis");
            }
            Map<String, Object> parsed = objectMapper.readValue(jsonText, new TypeReference<Map<String, Object>>() {
            });
            return normalizeAnalysis(parsed, skills, questionAnswers);
        } catch (JacksonException exception) {
            log.warn("Failed to parse Gemini analysis response, using fallback analysis: {}", exception.getMessage());
            return buildFallbackAnalysis(skills, questionAnswers);
        } catch (Exception exception) {
            log.warn("Failed to generate mock interview analysis using Gemini, using fallback analysis: {}", exception.getMessage());
            return buildFallbackAnalysis(skills, questionAnswers);
        }
    }

    private Map<String, Object> callGemini(String prompt,
                                           int maxOutputTokens,
                                           String responseMimeType,
                                           Map<String, Object> responseSchema) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(geminiProperties.apiKeyHeaderName(), geminiProperties.apiKey());

        Map<String, Object> generationConfig;
        if (responseSchema == null) {
            generationConfig = Map.of(
                    "responseMimeType", responseMimeType,
                    "temperature", 0.5,
                    "maxOutputTokens", maxOutputTokens
            );
        } else {
            generationConfig = Map.of(
                    "responseMimeType", responseMimeType,
                    "responseSchema", responseSchema,
                    "temperature", 0.5,
                    "maxOutputTokens", maxOutputTokens
            );
        }

        Map<String, Object> payload = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", generationConfig
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = mlRestTemplate.exchange(
                geminiProperties.baseUrl() + "/models/" + geminiProperties.model() + ":generateContent",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                Map.class
        ).getBody();
        return response;
    }

    private Map<String, Object> buildAnalysisResponseSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "score", Map.of("type", "INTEGER"),
                        "feedback", Map.of("type", "STRING"),
                        "technicalAnalysis", Map.of("type", "STRING"),
                        "behavioralAnalysis", Map.of("type", "STRING"),
                        "overallAssessment", Map.of("type", "STRING"),
                        "strengths", Map.of(
                                "type", "ARRAY",
                                "items", Map.of("type", "STRING")
                        ),
                        "weaknesses", Map.of(
                                "type", "ARRAY",
                                "items", Map.of("type", "STRING")
                        )
                ),
                "required", List.of("score", "feedback", "technicalAnalysis", "behavioralAnalysis", "overallAssessment", "strengths", "weaknesses"),
                "propertyOrdering", List.of("score", "feedback", "technicalAnalysis", "behavioralAnalysis", "overallAssessment", "strengths", "weaknesses")
        );
    }

    private String buildQuestionPrompt(List<String> skills) {
        String joinedSkills = skills.stream().collect(Collectors.joining(", "));
        return """
                You are an expert technical interviewer and behavioral analyst.

                Your task is to generate 10 short, clear, and progressive mock interview questions for a candidate.

                Context:
                - The candidate's skills are extracted from their resume.
                - You MUST base your questions primarily on these skills.

                Candidate Skills:
                %s

                IMPORTANT RULES:

                1. Generate exactly 10 questions.

                2. Questions must be SHORT and concise:
                   - Maximum 1–2 lines per question
                   - No long paragraphs

                3. Questions must be skill-focused:
                   - Directly relate to the provided skills
                   - Prefer practical, real-world usage over theory

                4. Difficulty progression:
                   - Questions 1–3: Easy (basics, understanding of skills)
                   - Questions 4–7: Medium (practical scenarios, usage)
                   - Questions 8–10: Medium+ (decision making, trade-offs)
                   - Do NOT generate very hard or complex questions

                5. Mix of question types:
                   - Technical (based on listed skills)
                   - Behavioral (related to real work situations)
                   - Situational (problem-solving using those skills)

                6. Avoid:
                   - Very long questions
                   - Complex algorithms
                   - Competitive programming style questions
                   - Generic questions not related to skills

                7. Make questions feel like a real interviewer conversation.

                8. Output format:
                   - Return exactly 10 lines
                   - Each line must be in this format:
                     1. question text
                     2. question text
                   - Do not return JSON
                   - Do not return bullet points
                   - Do not return explanations
                   - Do not return any heading before or after the 10 lines

                Goal:
                Evaluate the candidate’s:
                - Practical knowledge of their own skills
                - Problem-solving ability
                - Communication
                - Real-world thinking
                """.formatted(joinedSkills);
    }

    private String buildAnalysisPrompt(List<String> skills, List<Map<String, Object>> questionAnswers) throws JacksonException {
        String joinedSkills = skills.stream().collect(Collectors.joining(", "));
        String qaJson = objectMapper.writeValueAsString(questionAnswers);
        return """
                You are an expert technical interviewer and behavioral analyst.

                Analyze the candidate's mock interview performance using the provided resume skills and question-answer transcript.

                Candidate skills:
                %s

                Interview questions and answers:
                %s

                CRITICAL OUTPUT RULES:
                - Return ONLY one valid JSON object.
                - Do NOT return markdown.
                - Do NOT wrap the JSON in backticks.
                - Do NOT add any intro, heading, explanation, note, or trailing text.
                - Do NOT omit any required key.
                - All string values must be properly quoted JSON strings.
                - Arrays must be valid JSON arrays of strings.
                - `score` must be an integer between 0 and 100.
                - `strengths` must contain 2 to 4 short string points.
                - `weaknesses` must contain 2 to 4 short string points.
                - `feedback`, `technicalAnalysis`, `behavioralAnalysis`, and `overallAssessment` must each be a single concise paragraph.
                - If you are unsure, still return valid JSON using best-effort values.

                Return exactly this JSON shape and nothing else:
                {
                  "score": 78,
                  "feedback": "Concise overall feedback here.",
                  "technicalAnalysis": "Concise technical analysis here.",
                  "behavioralAnalysis": "Concise behavioral analysis here.",
                  "overallAssessment": "Concise overall assessment here.",
                  "strengths": ["Point 1", "Point 2"],
                  "weaknesses": ["Point 1", "Point 2"]
                }

                Evaluation rules:
                - Be concise, recruiter-friendly, and realistic.
                - Evaluate clarity, confidence, communication, practical skill usage, and problem-solving.
                - Base the response only on the provided skills and answers.
                """.formatted(joinedSkills, qaJson);
    }

    @SuppressWarnings("unchecked")
    private String extractGeneratedText(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object candidatesObject = response.get("candidates");
        if (!(candidatesObject instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }
        Object firstCandidate = candidates.get(0);
        if (!(firstCandidate instanceof Map<?, ?> candidateMap)) {
            return null;
        }
        Object contentObject = candidateMap.get("content");
        if (!(contentObject instanceof Map<?, ?> contentMap)) {
            return null;
        }
        Object partsObject = contentMap.get("parts");
        if (!(partsObject instanceof List<?> parts) || parts.isEmpty()) {
            return null;
        }
        Object firstPart = parts.get(0);
        if (!(firstPart instanceof Map<?, ?> partMap)) {
            return null;
        }
        Object text = partMap.get("text");
        return text != null ? String.valueOf(text) : null;
    }

    private String normalizeJsonText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```(?:json)?\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }
        int firstBrace = normalized.indexOf('{');
        int lastBrace = normalized.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            normalized = normalized.substring(firstBrace, lastBrace + 1);
        }
        return normalized.trim();
    }

    private void validateQuestions(MockInterviewQuestionResponse response) {
        if (response == null || response.questions() == null || response.questions().size() != 10) {
            throw new BadRequestException("Gemini must return exactly 10 mock interview questions");
        }
    }

    private MockInterviewQuestionResponse ensureTenQuestions(MockInterviewQuestionResponse response, List<String> skills) {
        List<MockInterviewQuestionResponse.QuestionItem> current = new java.util.ArrayList<>();
        if (response != null && response.questions() != null) {
            current.addAll(response.questions());
        }

        List<String> normalizedSkills = skills.stream()
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .distinct()
                .toList();
        if (normalizedSkills.isEmpty()) {
            normalizedSkills = List.of("your core technical skills");
        }

        List<String> fallbackQuestions = List.of(
                "Can you explain how you have used %s in a real project?",
                "What is one core concept in %s that you use confidently?",
                "If you had to teach %s to a teammate, what would you explain first?",
                "Tell me about a practical problem you solved using %s.",
                "How would you debug an issue in a feature built with %s?",
                "Describe a situation where %s helped you improve reliability or performance.",
                "When using %s, how do you balance speed and code quality?",
                "If two solutions are possible in %s, how would you choose between them?",
                "Tell me about a team situation where you had to explain a %s decision clearly.",
                "If a production issue involved %s, what would your first steps be?"
        );

        int index = current.size();
        while (current.size() < 10) {
            String skill = normalizedSkills.get(Math.min(index % normalizedSkills.size(), normalizedSkills.size() - 1));
            String question = fallbackQuestions.get(current.size()).formatted(skill);
            current.add(new MockInterviewQuestionResponse.QuestionItem(current.size() + 1, question));
            index++;
        }

        if (current.size() > 10) {
            current = new java.util.ArrayList<>(current.subList(0, 10));
        }

        List<MockInterviewQuestionResponse.QuestionItem> renumbered = new java.util.ArrayList<>();
        for (int i = 0; i < current.size(); i++) {
            renumbered.add(new MockInterviewQuestionResponse.QuestionItem(i + 1, current.get(i).question()));
        }
        return new MockInterviewQuestionResponse(renumbered);
    }

    private MockInterviewQuestionResponse parseQuestionsFromText(String text) {
        List<String> lines = text.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        List<MockInterviewQuestionResponse.QuestionItem> questions = new java.util.ArrayList<>();
        for (String line : lines) {
            String cleaned = line.replaceFirst("^\\d+[.)-]\\s*", "").trim();
            if (!cleaned.isBlank()) {
                questions.add(new MockInterviewQuestionResponse.QuestionItem(questions.size() + 1, cleaned));
            }
        }
        if (questions.size() > 10) {
            questions = new java.util.ArrayList<>(questions.subList(0, 10));
        }
        return new MockInterviewQuestionResponse(questions);
    }

    private Map<String, Object> normalizeAnalysis(Map<String, Object> parsed,
                                                  List<String> skills,
                                                  List<Map<String, Object>> questionAnswers) {
        if (parsed == null || parsed.isEmpty()) {
            return buildFallbackAnalysis(skills, questionAnswers);
        }

        Integer score = extractInteger(parsed.get("score"));
        String feedback = extractString(parsed.get("feedback"));
        String technicalAnalysis = extractString(parsed.get("technicalAnalysis"));
        String behavioralAnalysis = extractString(parsed.get("behavioralAnalysis"));
        String overallAssessment = extractString(parsed.get("overallAssessment"));
        List<String> strengths = toStringList(parsed.get("strengths"));
        List<String> weaknesses = toStringList(parsed.get("weaknesses"));

        Map<String, Object> fallback = buildFallbackAnalysis(skills, questionAnswers);
        return Map.of(
                "score", score != null ? score : fallback.get("score"),
                "feedback", !feedback.isBlank() ? feedback : fallback.get("feedback"),
                "technicalAnalysis", !technicalAnalysis.isBlank() ? technicalAnalysis : fallback.get("technicalAnalysis"),
                "behavioralAnalysis", !behavioralAnalysis.isBlank() ? behavioralAnalysis : fallback.get("behavioralAnalysis"),
                "overallAssessment", !overallAssessment.isBlank() ? overallAssessment : fallback.get("overallAssessment"),
                "strengths", strengths.isEmpty() ? fallback.get("strengths") : strengths,
                "weaknesses", weaknesses.isEmpty() ? fallback.get("weaknesses") : weaknesses
        );
    }

    private Map<String, Object> buildFallbackAnalysis(List<String> skills, List<Map<String, Object>> questionAnswers) {
        int totalQuestions = Math.max(questionAnswers.size(), 1);
        long substantiveAnswers = questionAnswers.stream()
                .map(answer -> String.valueOf(answer.getOrDefault("answer", "")).trim())
                .filter(answer -> !answer.isBlank())
                .count();
        long detailedAnswers = questionAnswers.stream()
                .map(answer -> String.valueOf(answer.getOrDefault("answer", "")).trim())
                .filter(answer -> answer.length() >= 35)
                .count();

        int score = Math.max(55, Math.min(95, (int) Math.round((substantiveAnswers * 60.0 / totalQuestions) + (detailedAnswers * 40.0 / totalQuestions))));
        String joinedSkills = skills == null || skills.isEmpty() ? "the candidate's core skills" : String.join(", ", skills);

        String technicalAnalysis = detailedAnswers >= Math.max(3, totalQuestions / 2)
                ? "The candidate showed practical familiarity with " + joinedSkills + " and was able to discuss their usage with reasonable clarity."
                : "The candidate showed basic familiarity with " + joinedSkills + ", but several answers need more technical depth and clearer real-world examples.";

        String behavioralAnalysis = substantiveAnswers >= Math.max(7, totalQuestions - 2)
                ? "The candidate communicated consistently and attempted to answer each question with ownership and structure."
                : "The candidate completed the interview, but some responses were too brief and could be more structured and confident.";

        List<String> strengths = new java.util.ArrayList<>();
        if (substantiveAnswers >= Math.max(7, totalQuestions - 2)) {
            strengths.add("Answered most questions clearly");
        }
        if (detailedAnswers >= Math.max(3, totalQuestions / 2)) {
            strengths.add("Provided practical examples");
        }
        strengths.add("Completed the full mock interview");

        List<String> weaknesses = new java.util.ArrayList<>();
        if (detailedAnswers < Math.max(3, totalQuestions / 2)) {
            weaknesses.add("Needs more technical depth in answers");
        }
        if (substantiveAnswers < Math.max(7, totalQuestions - 2)) {
            weaknesses.add("Some responses are too short");
        }
        weaknesses.add("Can improve trade-off explanation and impact-focused storytelling");

        String feedback = score >= 75
                ? "The candidate completed the interview well and showed practical readiness, with room to make answers more impact-focused."
                : "The candidate completed the interview, but should improve answer depth, structure, and concrete examples before a real interview.";

        String overallAssessment = score >= 75
                ? "The candidate appears reasonably prepared for a practical interview round."
                : "The candidate has a base level of readiness, but would benefit from more practice before interviewing.";

        return Map.of(
                "score", score,
                "feedback", feedback,
                "technicalAnalysis", technicalAnalysis,
                "behavioralAnalysis", behavioralAnalysis,
                "overallAssessment", overallAssessment,
                "strengths", strengths.stream().distinct().limit(3).toList(),
                "weaknesses", weaknesses.stream().distinct().limit(3).toList()
        );
    }

    private Integer extractInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String extractString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        return List.of();
    }
}
