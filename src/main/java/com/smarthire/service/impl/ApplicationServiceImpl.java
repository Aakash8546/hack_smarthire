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
import com.smarthire.entity.enums.JobStatus;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.ChatRepository;
import com.smarthire.repository.JobApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.ApplicationService;
import com.smarthire.service.EmailService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ChatRepository chatRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public ApplicationResponse applyToJob(Long jobId, ApplyJobRequest request) {
        User candidate = getCurrentCandidate();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new BadRequestException("Only active jobs can be applied to");
        }
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
        log.info("Candidate {} applied to job {}", candidate.getEmail(), job.getId());
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
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForJob(Long jobId) {
        User recruiter = getCurrentRecruiter();
        Job job = jobRepository.findByIdAndRecruiter(jobId, recruiter)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found for recruiter"));
        return jobApplicationRepository.findAllByJobAndJobRecruiterOrderByCreatedAtDesc(job, recruiter).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ApplicationResponse updateStatus(Long applicationId, UpdateApplicationStatusRequest request) {
        User recruiter = getCurrentRecruiter();
        JobApplication application = jobApplicationRepository.findByIdAndJobRecruiter(applicationId, recruiter)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found for recruiter"));
        ApplicationStatus normalizedStatus = normalizeStatus(request.status());
        application.setStatus(normalizedStatus);
        JobApplication saved = jobApplicationRepository.save(application);
        emailService.sendApplicationStatusUpdatedEmail(
                saved.getCandidate().getEmail(),
                saved.getCandidate().getName(),
                saved.getJob().getTitle(),
                saved.getJob().getRecruiter() != null ? saved.getJob().getRecruiter().getName() : saved.getJob().getCompany(),
                saved.getStatus(),
                request.message()
        );
        log.info("Recruiter {} updated application {} status to {}", recruiter.getEmail(), applicationId, normalizedStatus);
        return toResponse(saved);
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
        Resume resume = application.getCandidate().getResume();
        return new ApplicationResponse(application.getId(), application.getCandidate().getId(), application.getCandidate().getName(),
                application.getCandidate().getEmail(), application.getCandidate().getSkills(),
                resume != null ? resume.getId() : null,
                application.getJob().getId(), application.getJob().getTitle(), toApiStatus(application.getStatus()), application.getCoverLetter(),
                application.getChat() != null ? application.getChat().getId() : null, application.getCreatedAt());
    }

    private ApplicationStatus normalizeStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new BadRequestException("Application status is required");
        }
        String normalized = rawStatus.trim().toUpperCase();
        if ("INTERVIEW".equals(normalized)) {
            return ApplicationStatus.CALLED_FOR_INTERVIEW;
        }
        try {
            return ApplicationStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid application status: " + rawStatus);
        }
    }

    private String toApiStatus(ApplicationStatus status) {
        if (status == ApplicationStatus.CALLED_FOR_INTERVIEW) {
            return "INTERVIEW";
        }
        return status.name();
    }
}
