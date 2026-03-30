package com.smarthire.service.impl;

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

@Service
@RequiredArgsConstructor
public class SkillGapServiceImpl implements SkillGapService {

    private final SkillGapAnalysisRepository skillGapAnalysisRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final MlIntegrationService mlIntegrationService;

    @Override
    @Transactional
    public SkillGapResponse analyzeSkillGap(Long candidateId, Long jobId) {
        User recruiter = getCurrentRecruiter();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new BadRequestException("You can analyze skill gaps only for your own jobs");
        }
        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        Resume resume = resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new BadRequestException("Candidate resume not found"));
        MlDtos.SkillGapResult result = mlIntegrationService.analyzeSkillGap(resume, job);
        SkillGapAnalysis analysis = new SkillGapAnalysis();
        analysis.setCandidate(candidate);
        analysis.setJob(job);
        analysis.setMissingSkills(result.missingSkills());
        analysis.setRoadmap(result.roadmap());
        SkillGapAnalysis savedAnalysis = skillGapAnalysisRepository.save(analysis);
        return new SkillGapResponse(savedAnalysis.getId(), job.getId(), job.getTitle(), savedAnalysis.getMissingSkills(),
                savedAnalysis.getRoadmap(), savedAnalysis.getCreatedAt());
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
