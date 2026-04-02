package com.smarthire.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.smarthire.dto.candidate.JobRecommendationResponse;
import com.smarthire.dto.common.ApplicationResponse;
import com.smarthire.dto.common.JobResponse;
import com.smarthire.dto.recruiter.CreateJobRequest;
import com.smarthire.entity.Job;
import com.smarthire.entity.JobApplication;
import com.smarthire.entity.Resume;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.ApplicationStatus;
import com.smarthire.entity.enums.JobStatus;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.JobApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.EmailService;
import com.smarthire.service.JobService;
import com.smarthire.service.ml.MlDtos;
import com.smarthire.service.ml.MlIntegrationService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final MlIntegrationService mlIntegrationService;
    private final EmailService emailService;

    @Override
    @Transactional
    public JobResponse createJob(CreateJobRequest request) {
        User recruiter = getCurrentRecruiter();
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setCompany(request.getCompany() == null || request.getCompany().isBlank() ? recruiter.getName() : request.getCompany().trim());
        job.setLocation(request.getLocation());
        job.setDescription(request.getDescription());
        job.setMinimumExperience(request.getExperience());
        job.setJobPackage(request.getJobPackage().trim());
        job.setRequiredSkills(request.getRequiredSkills().stream().map(String::trim).filter(skill -> !skill.isBlank()).distinct().toList());
        job.setStatus(JobStatus.ACTIVE);
        job.setRecruiter(recruiter);
        Job savedJob = jobRepository.save(job);
        notifyCandidatesAboutJob(savedJob);
        log.info("Recruiter {} created job {} with status {}", recruiter.getEmail(), savedJob.getId(), savedJob.getStatus());
        return toJobResponse(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobs() {
        return jobRepository.findAllByStatusOrderByCreatedAtDesc(JobStatus.ACTIVE).stream()
                .map(this::toJobResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getMyJobs() {
        User recruiter = getCurrentRecruiter();
        return jobRepository.findAllByRecruiterOrderByCreatedAtDesc(recruiter).stream()
                .map(this::toJobResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForJob(Long jobId) {
        User recruiter = getCurrentRecruiter();
        Job job = jobRepository.findByIdAndRecruiter(jobId, recruiter)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found for recruiter"));
        return jobApplicationRepository.findAllByJobAndJobRecruiterOrderByCreatedAtDesc(job, recruiter).stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    @Override
    @Transactional
    public JobResponse toggleJobStatus(Long jobId) {
        User recruiter = getCurrentRecruiter();
        Job job = jobRepository.findByIdAndRecruiter(jobId, recruiter)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found for recruiter"));
        job.setStatus(job.getStatus() == JobStatus.ACTIVE ? JobStatus.INACTIVE : JobStatus.ACTIVE);
        log.info("Recruiter {} changed job {} status to {}", recruiter.getEmail(), job.getId(), job.getStatus());
        return toJobResponse(jobRepository.save(job));
    }

    @Override
    @Transactional(readOnly = true)
    public JobRecommendationResponse recommendJobs() {
        User candidate = getCurrentCandidate();
        Resume resume = resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new BadRequestException("Upload a resume before requesting recommendations"));
        List<Job> jobs = jobRepository.findAllByStatusOrderByCreatedAtDesc(JobStatus.ACTIVE);
        MlDtos.RecommendationResult recommendationResult = mlIntegrationService.recommendJobs(resume, jobs);
        Function<Job, JobResponse> mapper = this::toJobResponse;
        List<JobResponse> recommendedJobs = jobs.stream()
                .filter(job -> recommendationResult.recommendedJobIds().contains(job.getId()))
                .sorted((left, right) -> Integer.compare(
                        recommendationResult.recommendedJobIds().indexOf(left.getId()),
                        recommendationResult.recommendedJobIds().indexOf(right.getId())))
                .map(mapper)
                .collect(Collectors.toList());
        return new JobRecommendationResponse(recommendedJobs, recommendationResult.reason());
    }

    private User getCurrentRecruiter() {
        User user = userRepository.findById(SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.RECRUITER) {
            throw new BadRequestException("Only recruiters can perform this action");
        }
        return user;
    }

    private User getCurrentCandidate() {
        User user = userRepository.findById(SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.CANDIDATE) {
            throw new BadRequestException("Only candidates can perform this action");
        }
        return user;
    }

    private JobResponse toJobResponse(Job job) {
        return new JobResponse(job.getId(), job.getTitle(), job.getCompany(), job.getLocation(), job.getDescription(),
                job.getMinimumExperience(), job.getMinimumExperience(), job.getRequiredSkills(), job.getJobPackage(),
                job.getStatus() != null ? job.getStatus().name() : null, job.getRecruiter().getId(), job.getRecruiter().getName(),
                job.getCreatedAt());
    }

    private void notifyCandidatesAboutJob(Job job) {
        userRepository.findAllByRoleAndVerifiedTrue(UserRole.CANDIDATE)
                .forEach(candidate -> emailService.sendJobPostedEmail(
                        candidate.getEmail(),
                        candidate.getName(),
                        job.getTitle(),
                        job.getRecruiter() != null ? job.getRecruiter().getName() : job.getCompany(),
                        job.getJobPackage(),
                        job.getLocation()
                ));
    }

    private ApplicationResponse toApplicationResponse(JobApplication application) {
        return new ApplicationResponse(
                application.getId(),
                application.getCandidate().getId(),
                application.getCandidate().getName(),
                application.getCandidate().getEmail(),
                application.getCandidate().getSkills(),
                application.getCandidate().getResume() != null ? application.getCandidate().getResume().getId() : null,
                application.getJob().getId(),
                application.getJob().getTitle(),
                application.getStatus() == ApplicationStatus.CALLED_FOR_INTERVIEW ? "INTERVIEW" : application.getStatus().name(),
                application.getCoverLetter(),
                application.getChat() != null ? application.getChat().getId() : null,
                application.getCreatedAt()
        );
    }
}
