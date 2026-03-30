package com.smarthire.service;

import com.smarthire.dto.candidate.MockInterviewResponse;
import com.smarthire.dto.recruiter.VideoInterviewResponse;

public interface InterviewService {

    MockInterviewResponse createMockInterview();

    VideoInterviewResponse scheduleVideoInterview(Long applicationId);
}
