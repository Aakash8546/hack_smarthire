package com.smarthire.service.ml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.smarthire.config.properties.MlApiProperties;
import com.smarthire.entity.Job;
import com.smarthire.entity.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MlIntegrationService {

    private final RestTemplate mlRestTemplate;
    private final MlApiProperties mlApiProperties;

    public MlDtos.ResumeAnalysisResult analyzeResume(String fileName, byte[] fileBytes) {
        if (mlApiProperties.enabled()) {
            try {
                Map<String, Object> response = post("/resume/analyze", Map.of(
                        "fileName", fileName,
                        "contentBase64", java.util.Base64.getEncoder().encodeToString(fileBytes)
                ));
                return new MlDtos.ResumeAnalysisResult(
                        ((Number) response.getOrDefault("resumeScore", 75.0)).doubleValue(),
                        castList(response.get("skills")),
                        String.valueOf(response.getOrDefault("summary", "Resume analyzed by external ML service"))
                );
            } catch (Exception exception) {
                log.warn("ML resume analysis failed, using fallback: {}", exception.getMessage());
            }
        }

        String text = new String(fileBytes);
        Set<String> discoveredSkills = detectSkills(text);
        double score = Math.min(98.0, 55.0 + discoveredSkills.size() * 5.5);
        return new MlDtos.ResumeAnalysisResult(score, new ArrayList<>(discoveredSkills),
                "Fallback analysis completed. The resume shows strength in " +
                        (discoveredSkills.isEmpty() ? "general software engineering" : String.join(", ", discoveredSkills)) + ".");
    }

    public MlDtos.RecommendationResult recommendJobs(Resume resume, List<Job> jobs) {
        if (mlApiProperties.enabled()) {
            try {
                Map<String, Object> response = post("/jobs/recommend", Map.of(
                        "skills", resume.getExtractedSkills(),
                        "resumeScore", resume.getResumeScore(),
                        "jobs", jobs.stream().map(job -> Map.of(
                                "id", job.getId(),
                                "skills", job.getRequiredSkills(),
                                "title", job.getTitle())).toList()
                ));
                return new MlDtos.RecommendationResult(castLongList(response.get("jobIds")),
                        String.valueOf(response.getOrDefault("reason", "Recommended by external ML service")));
            } catch (Exception exception) {
                log.warn("ML recommendation failed, using fallback: {}", exception.getMessage());
            }
        }

        List<Long> ids = jobs.stream()
                .sorted(Comparator.comparingInt((Job job) -> overlapCount(resume.getExtractedSkills(), job.getRequiredSkills())).reversed())
                .limit(5)
                .map(Job::getId)
                .toList();
        return new MlDtos.RecommendationResult(ids, "Recommended using skill overlap and resume score fallback logic.");
    }

    public MlDtos.MockInterviewResult analyzeMockInterview(List<String> skills) {
        if (mlApiProperties.enabled()) {
            try {
                Map<String, Object> response = post("/interview/mock", Map.of("skills", skills));
                return new MlDtos.MockInterviewResult(
                        String.valueOf(response.getOrDefault("technicalAnalysis", "Technical analysis from ML")),
                        String.valueOf(response.getOrDefault("behavioralAnalysis", "Behavioral analysis from ML"))
                );
            } catch (Exception exception) {
                log.warn("ML mock interview failed, using fallback: {}", exception.getMessage());
            }
        }

        String joinedSkills = skills.isEmpty() ? "core engineering fundamentals" : String.join(", ", skills);
        return new MlDtos.MockInterviewResult(
                "Candidate appears strongest in " + joinedSkills + ". Recommended preparation: deepen problem solving, system design, and production debugging.",
                "Behavioral readiness is moderate to strong. Focus on concise storytelling, ownership examples, and measurable impact."
        );
    }

    public MlDtos.SkillGapResult analyzeSkillGap(Resume resume, Job job) {
        if (mlApiProperties.enabled()) {
            try {
                Map<String, Object> response = post("/skills/gap", Map.of(
                        "resumeSkills", resume.getExtractedSkills(),
                        "jobSkills", job.getRequiredSkills(),
                        "jobTitle", job.getTitle()
                ));
                return new MlDtos.SkillGapResult(castList(response.get("missingSkills")),
                        String.valueOf(response.getOrDefault("roadmap", "Roadmap from ML service")));
            } catch (Exception exception) {
                log.warn("ML skill gap analysis failed, using fallback: {}", exception.getMessage());
            }
        }

        List<String> missingSkills = job.getRequiredSkills().stream()
                .filter(skill -> resume.getExtractedSkills().stream().noneMatch(existing -> existing.equalsIgnoreCase(skill)))
                .toList();
        String roadmap = missingSkills.isEmpty()
                ? "No major skill gap detected. Focus on interview preparation and relevant project examples."
                : "Prioritize learning " + String.join(", ", missingSkills) + ". Build one portfolio project covering these areas, then rehearse role-specific interview questions.";
        return new MlDtos.SkillGapResult(missingSkills, roadmap);
    }

    public MlDtos.SpamDetectionResult detectSpam(String content) {
        if (mlApiProperties.enabled()) {
            try {
                Map<String, Object> response = post("/chat/spam-detect", Map.of("content", content));
                return new MlDtos.SpamDetectionResult(Boolean.parseBoolean(String.valueOf(response.getOrDefault("spam", false))));
            } catch (Exception exception) {
                log.warn("ML spam detection failed, using fallback: {}", exception.getMessage());
            }
        }
        String lower = content.toLowerCase(Locale.ROOT);
        boolean spam = lower.contains("buy now") || lower.contains("free money") || lower.contains("crypto scheme");
        return new MlDtos.SpamDetectionResult(spam);
    }

    public MlDtos.CheatingDetectionResult detectCheating(Job job, Resume resume) {
        if (mlApiProperties.enabled()) {
            try {
                Map<String, Object> response = post("/interview/cheating-detect", Map.of(
                        "jobTitle", job.getTitle(),
                        "resumeSkills", resume.getExtractedSkills()
                ));
                return new MlDtos.CheatingDetectionResult(String.valueOf(response.getOrDefault("result", "ML result unavailable")));
            } catch (Exception exception) {
                log.warn("ML cheating detection failed, using fallback: {}", exception.getMessage());
            }
        }
        return new MlDtos.CheatingDetectionResult("Initial cheating risk is low. Continue monitoring for suspicious tab switching, impersonation, or inconsistent answer depth.");
    }

    private Map<String, Object> post(String path, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return mlRestTemplate.exchange(
                mlApiProperties.baseUrl() + path,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {
                }).getBody();
    }

    private Set<String> detectSkills(String text) {
        List<String> dictionary = List.of("Java", "Spring Boot", "Spring Security", "PostgreSQL", "Hibernate",
                "Docker", "AWS", "React", "REST", "Microservices", "Kubernetes", "Kafka", "Redis", "JUnit", "Maven");
        String lower = text.toLowerCase(Locale.ROOT);
        return dictionary.stream()
                .filter(skill -> lower.contains(skill.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int overlapCount(List<String> left, List<String> right) {
        return (int) left.stream().filter(skill -> right.stream().anyMatch(required -> required.equalsIgnoreCase(skill))).count();
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Long> castLongList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(item -> Long.valueOf(String.valueOf(item))).toList();
        }
        return List.of();
    }
}
