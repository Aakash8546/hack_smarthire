package com.smarthire.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.smarthire.config.properties.AppProperties;
import com.smarthire.dto.candidate.ResumeUploadResponse;
import com.smarthire.entity.Resume;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.ResumeRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.ResumeService;
import com.smarthire.service.ml.MlDtos;
import com.smarthire.service.ml.MlIntegrationService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final MlIntegrationService mlIntegrationService;
    private final AppProperties appProperties;

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
            resume.setFilePath(targetPath.toString());
            resume.setResumeScore(analysisResult.score());
            resume.setExtractedSkills(new ArrayList<>(new LinkedHashSet<>(analysisResult.skills())));
            resume.setSummary(analysisResult.summary());
            Resume savedResume = resumeRepository.save(resume);

            candidate.setSkills(new ArrayList<>(savedResume.getExtractedSkills()));
            userRepository.save(candidate);

            return new ResumeUploadResponse(savedResume.getId(), savedResume.getFileName(), savedResume.getResumeScore(),
                    savedResume.getExtractedSkills(), savedResume.getSummary());
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
}
