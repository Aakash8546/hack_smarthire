package com.smarthire.service.ml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
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
                Map<String, Object> response = analyzeResumeWithMultipart(fileName, fileBytes);
                log.info("Raw ML resume analysis response: {}", response);
                Map<String, Object> results = extractResumeContractResults(response);
                List<String> skills = extractResumeContractSkills(response, results);
                Double experienceYears = extractResumeContractExperienceYears(response, results);
                Double score = extractResumeScore(response, results, skills);
                String analyzedFileName = extractResumeContractFileName(response, fileName);
                String summary = "Resume analyzed successfully using external ML model.";
                return new MlDtos.ResumeAnalysisResult(analyzedFileName, score, skills, summary, experienceYears, results, response);
            } catch (HttpStatusCodeException exception) {
                log.warn("ML resume analysis failed with HTTP {}. Response body: {}",
                        exception.getStatusCode(), exception.getResponseBodyAsString());
            } catch (ResourceAccessException exception) {
                log.warn("ML resume analysis resource access failure for {}{}: {}",
                        mlApiProperties.baseUrl(), mlApiProperties.resumeAnalyzePath(), exception.getMessage());
            } catch (Exception exception) {
                log.warn("ML resume analysis failed, using fallback. Cause: {}", exception.getMessage(), exception);
            }
        }

        String text = new String(fileBytes);
        Set<String> discoveredSkills = detectSkills(text);
        double score = Math.min(98.0, 55.0 + discoveredSkills.size() * 5.5);
        Map<String, Object> results = new HashMap<>();
        results.put("skills", new ArrayList<>(discoveredSkills));
        results.put("experience_years", 0.0);
        return new MlDtos.ResumeAnalysisResult(fileName, score, new ArrayList<>(discoveredSkills),
                "Fallback analysis completed. The resume shows strength in " +
                        (discoveredSkills.isEmpty() ? "general software engineering" : String.join(", ", discoveredSkills)) + ".",
                0.0, results, Map.of("file_name", fileName, "results", results));
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
        List<String> missingSkills = job.getRequiredSkills().stream()
                .filter(skill -> resume.getExtractedSkills().stream().noneMatch(existing -> existing.equalsIgnoreCase(skill)))
                .toList();
        if (missingSkills.isEmpty()) {
            return new MlDtos.SkillGapResult(
                    missingSkills,
                    "No major skill gap detected. Focus on interview preparation and relevant project examples.",
                    List.of()
            );
        }

        if (mlApiProperties.enabled()) {
            try {
                log.info("Sending missing skills {} to learning videos endpoint {}{}", missingSkills,
                        mlApiProperties.skillVideosBaseUrl(), mlApiProperties.skillVideosPath());
                Map<String, Object> response = postToAbsoluteUrl(
                        mlApiProperties.skillVideosBaseUrl() + mlApiProperties.skillVideosPath(),
                        Map.of("skills", missingSkills, "max_results", 3)
                );
                log.info("Raw ML skill videos response: {}", response);
                List<MlDtos.SkillLearningResource> resources = extractLearningResources(response);
                String roadmap = buildLearningRoadmap(missingSkills, resources);
                return new MlDtos.SkillGapResult(missingSkills, roadmap, resources);
            } catch (HttpStatusCodeException exception) {
                log.warn("ML skill videos lookup failed with HTTP {}. Response body: {}",
                        exception.getStatusCode(), exception.getResponseBodyAsString());
            } catch (ResourceAccessException exception) {
                log.warn("ML skill videos resource access failure for {}{}: {}",
                        mlApiProperties.skillVideosBaseUrl(), mlApiProperties.skillVideosPath(), exception.getMessage());
            } catch (Exception exception) {
                log.warn("ML skill videos lookup failed, using fallback roadmap. Cause: {}", exception.getMessage(), exception);
            }
        }

        String roadmap = "Prioritize learning " + String.join(", ", missingSkills)
                + ". Build one portfolio project covering these areas, then rehearse role-specific interview questions.";
        return new MlDtos.SkillGapResult(missingSkills, roadmap, List.of());
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
        addApiKeyHeader(headers);
        return mlRestTemplate.exchange(mlApiProperties.baseUrl() + path, HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class).getBody();
    }

    private Map<String, Object> postToAbsoluteUrl(String url, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addApiKeyHeader(headers);
        return mlRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class).getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> analyzeResumeWithMultipart(String fileName, byte[] fileBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        addApiKeyHeader(headers);

        ByteArrayResource resource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        log.info("Sending resume to ML endpoint {}{} with file {}", mlApiProperties.baseUrl(),
                mlApiProperties.resumeAnalyzePath(), fileName);

        return mlRestTemplate.exchange(
                mlApiProperties.baseUrl() + mlApiProperties.resumeAnalyzePath(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class).getBody();
    }

    private void addApiKeyHeader(HttpHeaders headers) {
        if (mlApiProperties.apiKey() != null && !mlApiProperties.apiKey().isBlank()) {
            headers.add(mlApiProperties.apiKeyHeaderName(), mlApiProperties.apiKey());
        }
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new HashMap<>();
            map.forEach((key, mapValue) -> normalized.put(String.valueOf(key), mapValue));
            return normalized;
        }
        return Map.of();
    }

    private Map<String, Object> extractPrimaryResults(Map<String, Object> response) {
        List<String> resultKeys = List.of("results", "result", "data", "analysis", "response");
        for (String key : resultKeys) {
            Map<String, Object> nested = extractMap(response.get(key));
            if (!nested.isEmpty()) {
                return nested;
            }
        }
        return response;
    }

    private Map<String, Object> extractResumeContractResults(Map<String, Object> response) {
        Map<String, Object> result = extractMap(response.get("result"));
        if (!result.isEmpty()) {
            return result;
        }
        result = extractMap(response.get("results"));
        if (!result.isEmpty()) {
            return result;
        }
        return extractPrimaryResults(response);
    }

    private List<String> extractResumeContractSkills(Map<String, Object> response, Map<String, Object> results) {
        List<String> skills = castList(results.get("skills"));
        if (!skills.isEmpty()) {
            return skills;
        }
        skills = castList(response.get("skills"));
        if (!skills.isEmpty()) {
            return skills;
        }
        return extractSkills(response, results);
    }

    private Double extractResumeContractExperienceYears(Map<String, Object> response, Map<String, Object> results) {
        Double value = extractDouble(results.get("experience_years"), null);
        if (value != null) {
            return value;
        }
        value = extractDouble(response.get("experience_years"), null);
        if (value != null) {
            return value;
        }
        return extractExperienceYears(response, results);
    }

    private String extractResumeContractFileName(Map<String, Object> response, String defaultValue) {
        return extractFirstString(response, defaultValue,
                "filename", "file_name", "fileName", "document_name", "documentName");
    }

    private List<String> extractSkills(Map<String, Object> response, Map<String, Object> results) {
        for (String key : List.of("skills", "extracted_skills", "extractedSkills", "technical_skills",
                "technicalSkills", "matched_skills", "matchedSkills")) {
            List<String> skills = castList(response.get(key));
            if (!skills.isEmpty()) {
                return skills;
            }
        }
        for (String key : List.of("skills", "extracted_skills", "extractedSkills", "technical_skills",
                "technicalSkills", "matched_skills", "matchedSkills")) {
            List<String> skills = castList(results.get(key));
            if (!skills.isEmpty()) {
                return skills;
            }
        }
        Object rawSkillsText = extractFirstPresent(response, results,
                "skills_text", "skillsText", "skill_summary", "skillSummary");
        if (rawSkillsText instanceof String rawText && !rawText.isBlank()) {
            return List.of(rawText.split(",")).stream()
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }
        return List.of();
    }

    private Double extractExperienceYears(Map<String, Object> response, Map<String, Object> results) {
        for (String key : List.of("experience_years", "experienceYears", "years_of_experience",
                "yearsOfExperience", "experience", "total_experience")) {
            Double value = extractDouble(response.get(key), null);
            if (value != null) {
                return value;
            }
        }
        for (String key : List.of("experience_years", "experienceYears", "years_of_experience",
                "yearsOfExperience", "experience", "total_experience")) {
            Double value = extractDouble(results.get(key), null);
            if (value != null) {
                return value;
            }
        }
        return 0.0;
    }

    private Double extractResumeScore(Map<String, Object> response, Map<String, Object> results, List<String> skills) {
        for (String key : List.of("resumeScore", "resume_score", "score", "match_score", "matchScore")) {
            Double value = extractDouble(response.get(key), null);
            if (value != null) {
                return value;
            }
        }
        for (String key : List.of("resumeScore", "resume_score", "score", "match_score", "matchScore")) {
            Double value = extractDouble(results.get(key), null);
            if (value != null) {
                return value;
            }
        }
        return Math.min(98.0, 55.0 + skills.size() * 5.5);
    }

    private Object extractFirstPresent(Map<String, Object> response, Map<String, Object> results, String... keys) {
        for (String key : keys) {
            if (response.containsKey(key) && response.get(key) != null) {
                return response.get(key);
            }
        }
        for (String key : keys) {
            if (results.containsKey(key) && results.get(key) != null) {
                return results.get(key);
            }
        }
        return null;
    }

    private String extractFirstString(Map<String, Object> response, String defaultValue, String... keys) {
        for (String key : keys) {
            Object value = response.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return defaultValue;
    }

    private Double extractDouble(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private List<MlDtos.SkillLearningResource> extractLearningResources(Map<String, Object> response) {
        Object resultsObject = response != null ? response.get("results") : null;
        if (!(resultsObject instanceof List<?> resultsList)) {
            return List.of();
        }
        List<MlDtos.SkillLearningResource> resources = new ArrayList<>();
        for (Object item : resultsList) {
            Map<String, Object> resourceMap = extractMap(item);
            if (resourceMap.isEmpty()) {
                continue;
            }
            List<MlDtos.LearningVideo> videos = castVideoList(resourceMap.get("videos"));
            resources.add(new MlDtos.SkillLearningResource(
                    String.valueOf(resourceMap.getOrDefault("skill", "")),
                    String.valueOf(resourceMap.getOrDefault("search_query", "")),
                    videos
            ));
        }
        return resources;
    }

    @SuppressWarnings("unchecked")
    private List<MlDtos.LearningVideo> castVideoList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<MlDtos.LearningVideo> videos = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> map = extractMap(item);
            if (map.isEmpty()) {
                continue;
            }
            videos.add(new MlDtos.LearningVideo(
                    String.valueOf(map.getOrDefault("title", "")),
                    String.valueOf(map.getOrDefault("url", "")),
                    String.valueOf(map.getOrDefault("channel", "")),
                    map.get("duration") != null ? String.valueOf(map.get("duration")) : null,
                    map.get("views") != null ? String.valueOf(map.get("views")) : null,
                    map.get("thumbnail") != null ? String.valueOf(map.get("thumbnail")) : null
            ));
        }
        return videos;
    }

    private String buildLearningRoadmap(List<String> missingSkills, List<MlDtos.SkillLearningResource> resources) {
        if (resources.isEmpty()) {
            return "Prioritize learning " + String.join(", ", missingSkills)
                    + ". Build one portfolio project covering these areas, then rehearse role-specific interview questions.";
        }

        List<String> steps = new ArrayList<>();
        for (MlDtos.SkillLearningResource resource : resources) {
            String firstVideo = resource.videos().isEmpty() ? null : resource.videos().get(0).title();
            if (firstVideo == null || firstVideo.isBlank()) {
                steps.add("Study " + resource.skill() + " using the recommended tutorials.");
            } else {
                steps.add("Learn " + resource.skill() + " starting with \"" + firstVideo + "\".");
            }
        }
        steps.add("After learning the missing skills, build one project that uses them together and revisit the job requirements.");
        return String.join(" ", steps);
    }
}
