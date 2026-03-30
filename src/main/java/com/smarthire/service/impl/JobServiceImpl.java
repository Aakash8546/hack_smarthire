package com.smarthire.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.smarthire.dto.candidate.JobRecommendationResponse;
import com.smarthire.dto.common.JobResponse;
import com.smarthire.dto.recruiter.CreateJobRequest;
import com.smarthire.entity.Job;
import com.smarthire.entity.Resume;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.JobService;
import com.smarthire.service.ml.MlDtos;
import com.smarthire.service.ml.MlIntegrationService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final MlIntegrationService mlIntegrationService;

    @Override
    @Transactional
    public JobResponse createJob(CreateJobRequest request) {
        User recruiter = getCurrentRecruiter();
        Job job = new Job();
        job.setTitle(request.title());
        job.setCompany(request.company());
        job.setLocation(request.location());
        job.setDescription(request.description());
        job.setMinimumExperience(request.minimumExperience());
        job.setRequiredSkills(request.requiredSkills());
        job.setRecruiter(recruiter);
        return toJobResponse(jobRepository.save(job));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
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
    public JobRecommendationResponse recommendJobs() {
        User candidate = getCurrentCandidate();
        Resume resume = resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new BadRequestException("Upload a resume before requesting recommendations"));
        List<Job> jobs = jobRepository.findAll();
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
                job.getMinimumExperience(), job.getRequiredSkills(), job.getRecruiter().getId(), job.getRecruiter().getName(),
                job.getCreatedAt());
    }
}
