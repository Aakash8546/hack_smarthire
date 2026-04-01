package com.smarthire.service;

import com.smarthire.dto.candidate.InterviewCompletionResponse;
import com.smarthire.dto.candidate.InterviewEndSessionResponse;
import com.smarthire.dto.candidate.MockInterviewQuestionResponse;
import com.smarthire.dto.candidate.MockInterviewResponse;
import com.smarthire.dto.candidate.MockInterviewResultResponse;
import com.smarthire.dto.candidate.StartMockInterviewResponse;
import com.smarthire.dto.candidate.SubmitMockInterviewAnswerRequest;
import com.smarthire.dto.candidate.SubmitMockInterviewAnswerResponse;
import com.smarthire.dto.recruiter.VideoInterviewResponse;

public interface InterviewService {

    MockInterviewResponse createMockInterview();

    MockInterviewQuestionResponse getMockInterviewQuestions();

    StartMockInterviewResponse startMockInterviewSession();

    SubmitMockInterviewAnswerResponse submitMockInterviewAnswer(Long interviewId, SubmitMockInterviewAnswerRequest request);

    MockInterviewResultResponse getMockInterviewResult(Long interviewId);

    InterviewEndSessionResponse endMockInterviewSession(Long interviewId);

    StartMockInterviewResponse startOrResumeCurrentSession();

    SubmitMockInterviewAnswerResponse submitCurrentSessionAnswer(SubmitMockInterviewAnswerRequest request);

    InterviewCompletionResponse isCurrentSessionComplete();

    MockInterviewResultResponse getCurrentSessionResult();

    InterviewEndSessionResponse endCurrentSession();

    VideoInterviewResponse scheduleVideoInterview(Long applicationId);
}
