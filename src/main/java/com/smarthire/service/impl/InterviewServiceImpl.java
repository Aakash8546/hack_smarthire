
package com.smarthire.service.impl;

import java.util.UUID;

import com.smarthire.config.properties.AppProperties;
import com.smarthire.dto.candidate.MockInterviewResponse;
import com.smarthire.dto.recruiter.VideoInterviewResponse;
import com.smarthire.entity.Interview;
import com.smarthire.entity.JobApplication;
import com.smarthire.entity.Resume;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.ApplicationStatus;
import com.smarthire.entity.enums.InterviewType;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.InterviewRepository;
import com.smarthire.repository.JobApplicationRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.InterviewService;
import com.smarthire.service.ml.MlDtos;
import com.smarthire.service.ml.MlIntegrationService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final MlIntegrationService mlIntegrationService;
    private final AppProperties appProperties;

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
        Interview savedInterview = interviewRepository.save(interview);
        return new MockInterviewResponse(savedInterview.getId(), savedInterview.getTechnicalAnalysis(),
                savedInterview.getBehavioralAnalysis(), savedInterview.getCreatedAt());
    }

    @Override
    @Transactional
    public VideoInterviewResponse scheduleVideoInterview(Long applicationId) {
        User recruiter = getCurrentRecruiter();
        JobApplication application = jobApplicationRepository.findByIdAndJobRecruiter(applicationId, recruiter)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (application.getStatus() != ApplicationStatus.CALLED_FOR_INTERVIEW) {
            throw new BadRequestException("Video interview can only be enabled when status is CALLED_FOR_INTERVIEW");
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
}
