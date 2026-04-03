package com.smarthire.controller;

import java.util.List;

import com.smarthire.dto.candidate.JobRecommendationResponse;
import com.smarthire.dto.candidate.MockInterviewQuestionResponse;
import com.smarthire.dto.candidate.MockInterviewResponse;
import com.smarthire.dto.candidate.MockInterviewResultResponse;
import com.smarthire.dto.candidate.ResumeAnalysisResponse;
import com.smarthire.dto.candidate.ResumeExistsResponse;
import com.smarthire.dto.candidate.ResumeUploadResponse;
import com.smarthire.dto.candidate.StartMockInterviewResponse;
import com.smarthire.dto.candidate.SubmitMockInterviewAnswerRequest;
import com.smarthire.dto.candidate.SubmitMockInterviewAnswerResponse;
import com.smarthire.dto.common.ApplicationResponse;
import com.smarthire.dto.common.JobResponse;
import com.smarthire.dto.recruiter.ApplyJobRequest;
import com.smarthire.dto.recruiter.SkillGapResponse;
import com.smarthire.service.ApplicationService;
import com.smarthire.service.InterviewService;
import com.smarthire.service.JobService;
import com.smarthire.service.ResumeService;
import com.smarthire.service.SkillGapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
public class CandidateController {

    private final ResumeService resumeService;
    private final JobService jobService;
    private final ApplicationService applicationService;
    private final InterviewService interviewService;
    private final SkillGapService skillGapService;

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResponse> uploadResume(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(resumeService.uploadResume(file));
    }

    @PostMapping(value = "/profile/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResponse> uploadResumeToProfile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(resumeService.uploadResume(file));
    }

    @DeleteMapping("/resume")
    public ResponseEntity<String> deleteResume() {
        return ResponseEntity.ok(resumeService.deleteResume());
    }

    @GetMapping("/profile/resume/exists")
    public ResponseEntity<ResumeExistsResponse> hasResume() {
        return ResponseEntity.ok(resumeService.hasResume(com.smarthire.util.SecurityUtils.getCurrentUser().getId()));
    }

    @GetMapping("/profile/resume/analysis")
    public ResponseEntity<ResumeAnalysisResponse> getResumeAnalysis() {
        return ResponseEntity.ok(resumeService.getResumeAnalysis(com.smarthire.util.SecurityUtils.getCurrentUser().getId()));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/jobs/recommendations")
    public ResponseEntity<JobRecommendationResponse> recommendJobs() {
        return ResponseEntity.ok(jobService.recommendJobs());
    }

    @PostMapping("/jobs/{jobId}/skill-gap")
    public ResponseEntity<SkillGapResponse> analyzeSkillGap(@PathVariable Long jobId) {
        return ResponseEntity.ok(skillGapService.analyzeSkillGap(jobId));
    }

    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<ApplicationResponse> applyToJob(@PathVariable Long jobId,
                                                          @Valid @RequestBody ApplyJobRequest request) {
        return ResponseEntity.ok(applicationService.applyToJob(jobId, request));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications() {
        return ResponseEntity.ok(applicationService.getMyApplications());
    }

    @PostMapping("/mock-interview")
    public ResponseEntity<MockInterviewResponse> createMockInterview() {
        return ResponseEntity.ok(interviewService.createMockInterview());
    }

    @GetMapping("/mock-interview/questions")
    public ResponseEntity<MockInterviewQuestionResponse> getMockInterviewQuestions() {
        return ResponseEntity.ok(interviewService.getMockInterviewQuestions());
    }

    @PostMapping("/mock-interview/session/start")
    public ResponseEntity<StartMockInterviewResponse> startMockInterviewSession() {
        return ResponseEntity.ok(interviewService.startMockInterviewSession());
    }

    @PostMapping("/mock-interview/session/{interviewId}/answer")
    public ResponseEntity<SubmitMockInterviewAnswerResponse> submitMockInterviewAnswer(@PathVariable Long interviewId,
                                                                                       @Valid @RequestBody SubmitMockInterviewAnswerRequest request) {
        return ResponseEntity.ok(interviewService.submitMockInterviewAnswer(interviewId, request));
    }

    @GetMapping("/mock-interview/session/{interviewId}/result")
    public ResponseEntity<MockInterviewResultResponse> getMockInterviewResult(@PathVariable Long interviewId) {
        return ResponseEntity.ok(interviewService.getMockInterviewResult(interviewId));
    }

    @DeleteMapping("/mock-interview/session/{interviewId}")
    public ResponseEntity<?> endMockInterviewSession(@PathVariable Long interviewId) {
        return ResponseEntity.ok(interviewService.endMockInterviewSession(interviewId));
    }
}