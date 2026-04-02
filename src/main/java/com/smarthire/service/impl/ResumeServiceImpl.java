package com.smarthire.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;

import com.smarthire.config.properties.AppProperties;
import com.smarthire.dto.candidate.ResumeAnalysisResponse;
import com.smarthire.dto.candidate.ResumeExistsResponse;
import com.smarthire.dto.candidate.ResumeUploadResponse;
import com.smarthire.dto.common.ResumeDownloadResponse;
import com.smarthire.entity.Resume;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.JobApplicationRepository;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.ResumeService;
import com.smarthire.service.ml.MlDtos;
import com.smarthire.service.ml.MlIntegrationService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final MlIntegrationService mlIntegrationService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ResumeUploadResponse uploadResume(MultipartFile file) {
        User candidate = getCurrentCandidate();
        validateFile(file);
        try {
            Path directory = Path.of(appProperties.file().uploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String savedFileName = candidate.getId() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path targetPath = directory.resolve(savedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            MlDtos.ResumeAnalysisResult analysisResult = mlIntegrationService.analyzeResume(savedFileName, file.getBytes());

            Resume resume = resumeRepository.findByCandidate(candidate).orElseGet(Resume::new);
            resume.setCandidate(candidate);
            resume.setFileName(savedFileName);
            resume.setOriginalFileName(file.getOriginalFilename());
            resume.setFilePath(targetPath.toString());
            resume.setResumeScore(analysisResult.score());
            resume.setExtractedSkills(new ArrayList<>(new LinkedHashSet<>(analysisResult.skills())));
            resume.setSummary(analysisResult.summary());
            resume.setExperienceYears(analysisResult.experienceYears());
            resume.setAnalysisResponseJson(writeJson(analysisResult.rawResponse()));
            Resume savedResume = resumeRepository.save(resume);

            candidate.setSkills(new ArrayList<>(savedResume.getExtractedSkills()));
            userRepository.save(candidate);

            return new ResumeUploadResponse(savedResume.getId(), savedResume.getFileName(), savedResume.getOriginalFileName(),
                    savedResume.getResumeScore(), savedResume.getExtractedSkills(), savedResume.getExperienceYears(),
                    analysisResult.results(), savedResume.getSummary());
        } catch (IOException exception) {
            throw new BadRequestException("Failed to store resume file: " + exception.getMessage());
        }
    }

    @Override
    @Transactional
    public String deleteResume() {
        User candidate = getCurrentCandidate();
        Resume resume = resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        try {
            Files.deleteIfExists(Path.of(resume.getFilePath()));
        } catch (IOException exception) {
            throw new BadRequestException("Failed to delete resume file: " + exception.getMessage());
        }
        resumeRepository.delete(resume);
        candidate.setSkills(new ArrayList<>());
        userRepository.save(candidate);
        return "Resume deleted successfully.";
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeDownloadResponse downloadResume(Long resumeId) {
        Resume resume = getAccessibleResumeByResumeId(resumeId);
        Resource resource = new FileSystemResource(resume.getFilePath());
        if (!resource.exists()) {
            throw new ResourceNotFoundException("Resume file not found on disk");
        }
        return new ResumeDownloadResponse(resource, resume.getOriginalFileName());
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeExistsResponse hasResume(Long candidateId) {
        User candidate = getAccessibleCandidate(candidateId);
        return resumeRepository.findByCandidate(candidate)
                .map(resume -> new ResumeExistsResponse(candidate.getId(), true, resume.getId(), resume.getOriginalFileName()))
                .orElseGet(() -> new ResumeExistsResponse(candidate.getId(), false, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeAnalysisResponse getResumeAnalysis(Long candidateId) {
        Resume resume = getAccessibleResume(candidateId);
        Map<String, Object> rawResponse = readJson(resume.getAnalysisResponseJson());
        Map<String, Object> results = readNestedResults(rawResponse);
        return new ResumeAnalysisResponse(resume.getId(), resume.getCandidate().getId(), resume.getFileName(),
                resume.getOriginalFileName(), resume.getResumeScore(), resume.getExtractedSkills(), resume.getExperienceYears(),
                results, rawResponse, resume.getSummary(), resume.getUpdatedAt());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Resume file is required");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF resumes are allowed");
        }
    }

    private User getCurrentCandidate() {
        User user = userRepository.findById(SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.CANDIDATE) {
            throw new BadRequestException("Only candidates can manage resumes");
        }
        return user;
    }

    private Resume getAccessibleResume(Long candidateId) {
        User candidate = getAccessibleCandidate(candidateId);
        return resumeRepository.findByCandidate(candidate)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
    }

    private Resume getAccessibleResumeByResumeId(Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        User currentUser = userRepository.findById(SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (currentUser.getRole() == UserRole.CANDIDATE) {
            if (!resume.getCandidate().getId().equals(currentUser.getId())) {
                throw new BadRequestException("Candidates can access only their own resume");
            }
            return resume;
        }
        if (currentUser.getRole() == UserRole.RECRUITER
                && !jobApplicationRepository.existsByCandidateAndJobRecruiter(resume.getCandidate(), currentUser)) {
            throw new BadRequestException("Recruiter is not authorized to access this resume");
        }
        return resume;
    }

    private User getAccessibleCandidate(Long candidateId) {
        User currentUser = userRepository.findById(SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (currentUser.getRole() == UserRole.CANDIDATE && !currentUser.getId().equals(candidateId)) {
            throw new BadRequestException("Candidates can access only their own resume");
        }
        return userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new BadRequestException("Failed to store ML response: " + exception.getMessage());
        }
    }

    private Map<String, Object> readJson(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {
            });
        } catch (JacksonException exception) {
            throw new BadRequestException("Failed to read stored ML response: " + exception.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readNestedResults(Map<String, Object> rawResponse) {
        Object results = rawResponse.get("results");
        if (results instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
