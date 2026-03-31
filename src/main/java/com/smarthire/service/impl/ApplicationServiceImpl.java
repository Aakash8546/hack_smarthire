package com.smarthire.service.impl;

import java.util.List;

import com.smarthire.dto.common.ApplicationResponse;
import com.smarthire.dto.recruiter.ApplyJobRequest;
import com.smarthire.dto.recruiter.UpdateApplicationStatusRequest;
import com.smarthire.entity.Chat;
import com.smarthire.entity.Job;
import com.smarthire.entity.JobApplication;
import com.smarthire.entity.Resume;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.ApplicationStatus;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.ChatRepository;
import com.smarthire.repository.JobApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.ApplicationService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ChatRepository chatRepository;

    @Override
    @Transactional
    public ApplicationResponse applyToJob(Long jobId, ApplyJobRequest request) {
        User candidate = getCurrentCandidate();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (jobApplicationRepository.existsByCandidateAndJob(candidate, job)) {
            throw new BadRequestException("You have already applied to this job");
        }
        Resume resume = resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new BadRequestException("Upload a resume before applying"));
        if (resume.getExtractedSkills().isEmpty()) {
            throw new BadRequestException("Resume analysis is required before applying");
        }
        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStatus(ApplicationStatus.PENDING);
        application.setCoverLetter(request.coverLetter());
        JobApplication savedApplication = jobApplicationRepository.save(application);

        Chat chat = new Chat();
        chat.setApplication(savedApplication);
        chat.setCandidate(candidate);
        chat.setRecruiter(job.getRecruiter());
        chatRepository.save(chat);

        savedApplication.setChat(chat);
        return toResponse(savedApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications() {
        User candidate = getCurrentCandidate();
        return jobApplicationRepository.findAllByCandidateOrderByCreatedAtDesc(candidate).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getRecruiterApplications() {
        User recruiter = getCurrentRecruiter();
        return jobApplicationRepository.findAllByJobRecruiterOrderByCreatedAtDesc(recruiter).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ApplicationResponse updateStatus(Long applicationId, UpdateApplicationStatusRequest request) {
        User recruiter = getCurrentRecruiter();
        JobApplication application = jobApplicationRepository.findByIdAndJobRecruiter(applicationId, recruiter)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found for recruiter"));
        application.setStatus(request.status());
        return toResponse(jobApplicationRepository.save(application));
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

    private ApplicationResponse toResponse(JobApplication application) {
        return new ApplicationResponse(application.getId(), application.getCandidate().getId(), application.getCandidate().getName(),
                application.getJob().getId(), application.getJob().getTitle(), application.getStatus(), application.getCoverLetter(),
                application.getChat() != null ? application.getChat().getId() : null, application.getCreatedAt());
    }
}
