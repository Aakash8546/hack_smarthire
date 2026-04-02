package com.smarthire.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.smarthire.config.properties.AppProperties;
import com.smarthire.dto.candidate.InterviewCompletionResponse;
import com.smarthire.dto.candidate.InterviewEndSessionResponse;
import com.smarthire.dto.candidate.MockInterviewQuestionResponse;
import com.smarthire.dto.candidate.MockInterviewResponse;
import com.smarthire.dto.candidate.MockInterviewResultResponse;
import com.smarthire.dto.candidate.StartMockInterviewResponse;
import com.smarthire.dto.candidate.SubmitMockInterviewAnswerRequest;
import com.smarthire.dto.candidate.SubmitMockInterviewAnswerResponse;
import com.smarthire.dto.recruiter.VideoInterviewResponse;
import com.smarthire.entity.Interview;
import com.smarthire.entity.JobApplication;
import com.smarthire.entity.Resume;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.ApplicationStatus;
import com.smarthire.entity.enums.InterviewType;
import com.smarthire.entity.enums.MockInterviewSessionStatus;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.InterviewRepository;
import com.smarthire.repository.JobApplicationRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.InterviewService;
import com.smarthire.service.ai.GeminiInterviewQuestionService;
import com.smarthire.service.ml.MlDtos;
import com.smarthire.service.ml.MlIntegrationService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final MlIntegrationService mlIntegrationService;
    private final GeminiInterviewQuestionService geminiInterviewQuestionService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MockInterviewResponse createMockInterview() {
        User candidate = getCurrentCandidate();
        Resume resume = resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new BadRequestException("Upload a resume before starting a mock interview"));
        MlDtos.MockInterviewResult analysis = mlIntegrationService.analyzeMockInterview(resume.getExtractedSkills());
        Interview interview = new Interview();
        interview.setCandidate(candidate);
        interview.setType(InterviewType.MOCK);
        interview.setTechnicalAnalysis(analysis.technicalAnalysis());
        interview.setBehavioralAnalysis(analysis.behavioralAnalysis());
        interview.setSessionStatus(MockInterviewSessionStatus.COMPLETED);
        interview.setCompleted(true);
        Interview savedInterview = interviewRepository.save(interview);
        return new MockInterviewResponse(savedInterview.getId(), savedInterview.getTechnicalAnalysis(),
                savedInterview.getBehavioralAnalysis(), savedInterview.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public MockInterviewQuestionResponse getMockInterviewQuestions() {
        User candidate = getCurrentCandidate();
        Resume resume = resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new BadRequestException("Upload a resume before generating mock interview questions"));
        List<String> skills = extractDistinctSkills(resume);
        if (skills.isEmpty()) {
            throw new BadRequestException("Resume analysis skills are required before generating mock interview questions");
        }
        return geminiInterviewQuestionService.generateQuestions(skills);
    }

    @Override
    @Transactional
    public StartMockInterviewResponse startMockInterviewSession() {
        return startOrResumeCurrentSession();
    }

    @Override
    @Transactional
    public StartMockInterviewResponse startOrResumeCurrentSession() {
        User candidate = getCurrentCandidate();
        Resume resume = resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new BadRequestException("Upload a resume before starting a mock interview"));
        List<String> skills = extractDistinctSkills(resume);
        if (skills.isEmpty()) {
            throw new BadRequestException("Resume analysis skills are required before starting a mock interview");
        }

        Interview activeSession = findOrNormalizeActiveSession(candidate);
        if (activeSession != null) {
            return buildStartResponse(activeSession, true);
        }

        MockInterviewQuestionResponse generated = geminiInterviewQuestionService.generateQuestions(skills);
        Interview interview = new Interview();
        interview.setCandidate(candidate);
        interview.setType(InterviewType.MOCK);
        interview.setMockInterviewQuestionsJson(writeJson(generated.questions()));
        interview.setMockInterviewAnswersJson(writeJson(List.of()));
        interview.setCurrentQuestionIndex(0);
        interview.setCompleted(false);
        interview.setSessionStatus(MockInterviewSessionStatus.ACTIVE);
        Interview saved = interviewRepository.save(interview);
        return buildStartResponse(saved, false);
    }

    @Override
    @Transactional
    public SubmitMockInterviewAnswerResponse submitMockInterviewAnswer(Long interviewId, SubmitMockInterviewAnswerRequest request) {
        User candidate = getCurrentCandidate();
        Interview interview = interviewRepository.findByIdAndCandidateAndType(interviewId, candidate, InterviewType.MOCK)
                .orElseThrow(() -> new ResourceNotFoundException("Mock interview session not found"));
        return submitAnswer(interview, request);
    }

    @Override
    @Transactional
    public SubmitMockInterviewAnswerResponse submitCurrentSessionAnswer(SubmitMockInterviewAnswerRequest request) {
        User candidate = getCurrentCandidate();
        Interview interview = requireActiveSession(candidate);
        return submitAnswer(interview, request);
    }

    @Override
    @Transactional(readOnly = true)
    public MockInterviewResultResponse getMockInterviewResult(Long interviewId) {
        User candidate = getCurrentCandidate();
        Interview interview = interviewRepository.findByIdAndCandidateAndType(interviewId, candidate, InterviewType.MOCK)
                .orElseThrow(() -> new ResourceNotFoundException("Mock interview session not found"));
        if (interview.getSessionStatus() != MockInterviewSessionStatus.COMPLETED) {
            throw new BadRequestException("Please complete all questions before viewing results");
        }
        return buildResultResponse(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewCompletionResponse isCurrentSessionComplete() {
        User candidate = getCurrentCandidate();
        Interview activeSession = findLatestActiveSession(candidate);
        if (activeSession != null) {
            return new InterviewCompletionResponse(false);
        }
        Interview completedSession = findLatestCompletedSession(candidate);
        return new InterviewCompletionResponse(completedSession != null);
    }

    @Override
    @Transactional
    public MockInterviewResultResponse getCurrentSessionResult() {
        User candidate = getCurrentCandidate();
        Interview activeSession = findLatestActiveSession(candidate);
        if (activeSession != null) {
            throw new BadRequestException("Please complete all questions before viewing results");
        }
        Interview completedSession = findLatestCompletedSession(candidate);
        if (completedSession == null) {
            throw new ResourceNotFoundException("No completed mock interview session found");
        }
        if (completedSession.getMockInterviewAnalysisJson() == null || completedSession.getMockInterviewAnalysisJson().isBlank()) {
            completedSession = completeInterviewAnalysis(completedSession, candidate);
        }
        return buildResultResponse(completedSession);
    }

    @Override
    @Transactional
    public InterviewEndSessionResponse endMockInterviewSession(Long interviewId) {
        User candidate = getCurrentCandidate();
        Interview interview = interviewRepository.findByIdAndCandidateAndType(interviewId, candidate, InterviewType.MOCK)
                .orElseThrow(() -> new ResourceNotFoundException("Mock interview session not found"));
        if (interview.getSessionStatus() == MockInterviewSessionStatus.TERMINATED) {
            return new InterviewEndSessionResponse("Mock interview session is already terminated.");
        }
        terminateSession(interview);
        return new InterviewEndSessionResponse("Mock interview session ended successfully.");
    }

    @Override
    @Transactional
    public InterviewEndSessionResponse endCurrentSession() {
        User candidate = getCurrentCandidate();
        Interview interview = findOrNormalizeActiveSession(candidate);
        if (interview == null) {
            Interview latestTerminated = findLatestTerminatedSession(candidate);
            if (latestTerminated != null) {
                return new InterviewEndSessionResponse("Mock interview session is already terminated.");
            }
            return new InterviewEndSessionResponse("No active mock interview session found.");
        }
        terminateSession(interview);
        return new InterviewEndSessionResponse("Mock interview session ended successfully.");
    }

    @Override
    @Transactional
    public VideoInterviewResponse scheduleVideoInterview(Long applicationId) {
        User recruiter = getCurrentRecruiter();
        JobApplication application = jobApplicationRepository.findByIdAndJobRecruiter(applicationId, recruiter)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (application.getStatus() != ApplicationStatus.INTERVIEW
                && application.getStatus() != ApplicationStatus.CALLED_FOR_INTERVIEW) {
            throw new BadRequestException("Video interview can only be enabled when status is INTERVIEW");
        }
        Resume resume = resumeRepository.findByCandidate(application.getCandidate())
                .orElseThrow(() -> new BadRequestException("Candidate resume not found"));
        MlDtos.CheatingDetectionResult cheatingDetectionResult =
                mlIntegrationService.detectCheating(application.getJob(), resume);

        String meetingRoomId = "smarthire-" + UUID.randomUUID();
        String meetingUrl = appProperties.video().baseUrl() + "/" + meetingRoomId;

        Interview interview = new Interview();
        interview.setCandidate(application.getCandidate());
        interview.setJob(application.getJob());
        interview.setType(InterviewType.VIDEO);
        interview.setMeetingRoomId(meetingRoomId);
        interview.setMeetingUrl(meetingUrl);
        interview.setCheatingDetectionResult(cheatingDetectionResult.result());
        Interview savedInterview = interviewRepository.save(interview);

        return new VideoInterviewResponse(savedInterview.getId(), applicationId, appProperties.video().provider(),
                savedInterview.getMeetingRoomId(), savedInterview.getMeetingUrl(),
                savedInterview.getCheatingDetectionResult(), savedInterview.getCreatedAt());
    }

    private SubmitMockInterviewAnswerResponse submitAnswer(Interview interview, SubmitMockInterviewAnswerRequest request) {
        if (interview.getSessionStatus() != MockInterviewSessionStatus.ACTIVE) {
            throw new BadRequestException("Mock interview session is not active");
        }

        List<MockInterviewQuestionResponse.QuestionItem> questions = readQuestions(interview.getMockInterviewQuestionsJson());
        List<Map<String, Object>> answers = readAnswerMaps(interview.getMockInterviewAnswersJson());
        int currentIndex = interview.getCurrentQuestionIndex() == null ? 0 : interview.getCurrentQuestionIndex();
        int expectedQuestionNumber = currentIndex + 1;

        if (currentIndex >= questions.size()) {
            throw new BadRequestException("No more questions left in this mock interview session");
        }
        if (request.questionNumber() != expectedQuestionNumber) {
            throw new BadRequestException("Please answer questions in sequence. Expected question number: " + expectedQuestionNumber);
        }
        if (answers.size() != currentIndex) {
            throw new BadRequestException("Session state is out of sync. Please refresh the interview session.");
        }

        MockInterviewQuestionResponse.QuestionItem currentQuestion = questions.get(currentIndex);
        answers.add(Map.of(
                "questionNumber", currentQuestion.questionNumber(),
                "question", currentQuestion.question(),
                "answer", request.answer()
        ));

        int nextIndex = currentIndex + 1;
        interview.setMockInterviewAnswersJson(writeJson(answers));
        interview.setCurrentQuestionIndex(nextIndex);

        if (nextIndex >= questions.size()) {
            Interview completedInterview = completeInterviewAnalysis(interview, interview.getCandidate());
            return new SubmitMockInterviewAnswerResponse(completedInterview.getId(), currentQuestion.questionNumber(),
                    null, questions.size(), null, true, true);
        }

        interviewRepository.save(interview);
        return new SubmitMockInterviewAnswerResponse(interview.getId(), currentQuestion.questionNumber(),
                nextIndex + 1, questions.size(), questions.get(nextIndex).question(), false, false);
    }

    private Interview completeInterviewAnalysis(Interview interview, User candidate) {
        List<Map<String, Object>> answers = readAnswerMaps(interview.getMockInterviewAnswersJson());
        Map<String, Object> analysis = geminiInterviewQuestionService.analyzeInterview(extractSkillList(candidate), answers);
        interview.setTechnicalAnalysis(String.valueOf(analysis.getOrDefault("technicalAnalysis", "Technical analysis unavailable.")));
        interview.setBehavioralAnalysis(String.valueOf(analysis.getOrDefault("behavioralAnalysis", "Behavioral analysis unavailable.")));
        interview.setMockInterviewAnalysisJson(writeJson(analysis));
        interview.setCompleted(true);
        interview.setSessionStatus(MockInterviewSessionStatus.COMPLETED);
        return interviewRepository.save(interview);
    }

    private void terminateSession(Interview interview) {
        interview.setSessionStatus(MockInterviewSessionStatus.TERMINATED);
        interview.setCompleted(false);
        interview.setCurrentQuestionIndex(0);
        interview.setMockInterviewQuestionsJson(null);
        interview.setMockInterviewAnswersJson(null);
        interview.setMockInterviewAnalysisJson(null);
        interview.setTechnicalAnalysis(null);
        interview.setBehavioralAnalysis(null);
        interviewRepository.save(interview);
    }

    private StartMockInterviewResponse buildStartResponse(Interview interview, boolean resumed) {
        List<MockInterviewQuestionResponse.QuestionItem> questions = readQuestions(interview.getMockInterviewQuestionsJson());
        int currentIndex = interview.getCurrentQuestionIndex() == null ? 0 : interview.getCurrentQuestionIndex();
        if (currentIndex >= questions.size()) {
            throw new BadRequestException("No pending questions found in the active session");
        }
        MockInterviewQuestionResponse.QuestionItem currentQuestion = questions.get(currentIndex);
        return new StartMockInterviewResponse(
                interview.getId(),
                currentQuestion.questionNumber(),
                questions.size(),
                currentQuestion.question(),
                interview.getSessionStatus().name(),
                resumed,
                interview.isCompleted(),
                interview.getSessionStatus() == MockInterviewSessionStatus.COMPLETED
        );
    }

    private MockInterviewResultResponse buildResultResponse(Interview interview) {
        List<MockInterviewQuestionResponse.QuestionItem> questions = readQuestions(interview.getMockInterviewQuestionsJson());
        List<Map<String, Object>> answers = readAnswerMaps(interview.getMockInterviewAnswersJson());
        List<MockInterviewResultResponse.QuestionAnswerItem> items = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            MockInterviewQuestionResponse.QuestionItem question = questions.get(index);
            String answer = index < answers.size() ? String.valueOf(answers.get(index).getOrDefault("answer", "")) : "";
            items.add(new MockInterviewResultResponse.QuestionAnswerItem(question.questionNumber(), question.question(), answer));
        }
        Map<String, Object> analysis = readMap(interview.getMockInterviewAnalysisJson());
        Integer score = extractInteger(analysis.get("score"));
        String feedback = String.valueOf(analysis.getOrDefault("feedback", analysis.getOrDefault("overallAssessment", "")));
        List<String> strengths = readStringList(analysis.get("strengths"));
        List<String> weaknesses = readStringList(analysis.getOrDefault("weaknesses", analysis.get("improvements")));

        return new MockInterviewResultResponse(
                interview.getId(),
                interview.getSessionStatus() != null ? interview.getSessionStatus().name() : null,
                interview.isCompleted(),
                score,
                feedback,
                interview.getTechnicalAnalysis(),
                interview.getBehavioralAnalysis(),
                strengths,
                weaknesses,
                analysis,
                items,
                interview.getUpdatedAt()
        );
    }

    private Interview findLatestActiveSession(User candidate) {
        return interviewRepository.findAllByCandidateAndTypeOrderByCreatedAtDesc(candidate, InterviewType.MOCK).stream()
                .filter(interview -> interview.getSessionStatus() == MockInterviewSessionStatus.ACTIVE)
                .findFirst()
                .orElse(null);
    }

    private Interview findOrNormalizeActiveSession(User candidate) {
        List<Interview> activeSessions = interviewRepository.findAllByCandidateAndTypeOrderByCreatedAtDesc(candidate, InterviewType.MOCK).stream()
                .filter(interview -> interview.getSessionStatus() == MockInterviewSessionStatus.ACTIVE)
                .toList();
        if (activeSessions.isEmpty()) {
            return null;
        }
        Interview latestActive = activeSessions.get(0);
        if (activeSessions.size() > 1) {
            for (int index = 1; index < activeSessions.size(); index++) {
                terminateSession(activeSessions.get(index));
            }
        }
        return latestActive;
    }

    private Interview findLatestCompletedSession(User candidate) {
        return interviewRepository.findAllByCandidateAndTypeOrderByCreatedAtDesc(candidate, InterviewType.MOCK).stream()
                .filter(interview -> interview.getSessionStatus() == MockInterviewSessionStatus.COMPLETED)
                .findFirst()
                .orElse(null);
    }

    private Interview findLatestTerminatedSession(User candidate) {
        return interviewRepository.findAllByCandidateAndTypeOrderByCreatedAtDesc(candidate, InterviewType.MOCK).stream()
                .filter(interview -> interview.getSessionStatus() == MockInterviewSessionStatus.TERMINATED)
                .findFirst()
                .orElse(null);
    }

    private Interview requireActiveSession(User candidate) {
        Interview interview = findOrNormalizeActiveSession(candidate);
        if (interview == null) {
            throw new ResourceNotFoundException("No active mock interview session found");
        }
        return interview;
    }

    private User getCurrentCandidate() {
        User user = userRepository.findById(SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.CANDIDATE) {
            throw new BadRequestException("Only candidates can perform this action");
        }
        return user;
    }

    private User getCurrentRecruiter() {
        User user = userRepository.findById(SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.RECRUITER) {
            throw new BadRequestException("Only recruiters can perform this action");
        }
        return user;
    }

    private List<String> extractSkillList(User candidate) {
        Resume resume = resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new BadRequestException("Resume not found for mock interview analysis"));
        return extractDistinctSkills(resume);
    }

    private List<String> extractDistinctSkills(Resume resume) {
        return new ArrayList<>(new LinkedHashSet<>(resume.getExtractedSkills()));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new BadRequestException("Failed to store mock interview data: " + exception.getMessage());
        }
    }

    private List<MockInterviewQuestionResponse.QuestionItem> readQuestions(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<MockInterviewQuestionResponse.QuestionItem>>() {
            });
        } catch (JacksonException exception) {
            throw new BadRequestException("Failed to read stored mock interview questions: " + exception.getMessage());
        }
    }

    private List<Map<String, Object>> readAnswerMaps(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(value, new TypeReference<List<Map<String, Object>>>() {
            }));
        } catch (JacksonException exception) {
            throw new BadRequestException("Failed to read stored mock interview answers: " + exception.getMessage());
        }
    }

    private Map<String, Object> readMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {
            });
        } catch (JacksonException exception) {
            throw new BadRequestException("Failed to read stored mock interview analysis: " + exception.getMessage());
        }
    }

    private Integer extractInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
