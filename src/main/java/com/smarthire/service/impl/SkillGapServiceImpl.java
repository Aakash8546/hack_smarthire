package com.smarthire.service.impl;

import java.util.List;

import com.smarthire.dto.recruiter.SkillGapResponse;
import com.smarthire.entity.Job;
import com.smarthire.entity.Resume;
import com.smarthire.entity.SkillGapAnalysis;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.SkillGapAnalysisRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.SkillGapService;
import com.smarthire.service.ml.MlDtos;
import com.smarthire.service.ml.MlIntegrationService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class SkillGapServiceImpl implements SkillGapService {

    private final SkillGapAnalysisRepository skillGapAnalysisRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final MlIntegrationService mlIntegrationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public SkillGapResponse analyzeSkillGap(Long jobId) {
        User candidate = getCurrentCandidate();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        Resume resume = resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new BadRequestException("Candidate resume not found"));
        MlDtos.SkillGapResult result = mlIntegrationService.analyzeSkillGap(resume, job);
        SkillGapAnalysis analysis = new SkillGapAnalysis();
        analysis.setCandidate(candidate);
        analysis.setJob(job);
        analysis.setMissingSkills(result.missingSkills());
        analysis.setRoadmap(result.roadmap());
        analysis.setLearningResourcesJson(writeJson(result.learningResources()));
        SkillGapAnalysis savedAnalysis = skillGapAnalysisRepository.save(analysis);
        return new SkillGapResponse(savedAnalysis.getId(), job.getId(), job.getTitle(), savedAnalysis.getMissingSkills(),
                savedAnalysis.getRoadmap(), mapLearningResources(result.learningResources()), savedAnalysis.getCreatedAt());
    }

    private User getCurrentCandidate() {
        User user = userRepository.findById(SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.CANDIDATE) {
            throw new BadRequestException("Only candidates can perform this action");
        }
        return user;
    }

    private String writeJson(List<MlDtos.SkillLearningResource> resources) {
        try {
            return objectMapper.writeValueAsString(resources);
        } catch (JacksonException exception) {
            throw new BadRequestException("Failed to store learning resources: " + exception.getMessage());
        }
    }

    private List<SkillGapResponse.LearningResource> mapLearningResources(List<MlDtos.SkillLearningResource> resources) {
        return resources.stream()
                .map(resource -> new SkillGapResponse.LearningResource(
                        resource.skill(),
                        resource.searchQuery(),
                        resource.videos().stream()
                                .map(video -> new SkillGapResponse.VideoResource(
                                        video.title(),
                                        video.url(),
                                        video.channel(),
                                        video.duration(),
                                        video.views(),
                                        video.thumbnail()
                                ))
                                .toList()
                ))
                .toList();
    }
}