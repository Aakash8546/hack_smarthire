package com.smarthire.controller;

import java.util.List;

import com.smarthire.dto.common.ApplicationResponse;
import com.smarthire.dto.common.JobResponse;
import com.smarthire.dto.recruiter.CreateJobRequest;
import com.smarthire.dto.recruiter.SkillGapResponse;
import com.smarthire.dto.recruiter.UpdateApplicationStatusRequest;
import com.smarthire.dto.recruiter.VideoInterviewResponse;
import com.smarthire.service.ApplicationService;
import com.smarthire.service.InterviewService;
import com.smarthire.service.JobService;
import com.smarthire.service.SkillGapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterController {

    private final JobService jobService;
    private final ApplicationService applicationService;
    private final SkillGapService skillGapService;
    private final InterviewService interviewService;

    @PostMapping("/jobs")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        return ResponseEntity.ok(jobService.createJob(request));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobResponse>> getMyJobs() {
        return ResponseEntity.ok(jobService.getMyJobs());
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsForJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(jobService.getApplicationsForJob(jobId));
    }

    @PutMapping("/jobs/{jobId}/status")
    public ResponseEntity<JobResponse> toggleJobStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(jobService.toggleJobStatus(jobId));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> getRecruiterApplications() {
        return ResponseEntity.ok(applicationService.getRecruiterApplications());
    }

    @PutMapping("/applications/{applicationId}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(@PathVariable Long applicationId,
                                                            @Valid @RequestBody UpdateApplicationStatusRequest request) {
        return ResponseEntity.ok(applicationService.updateStatus(applicationId, request));
    }

    @PostMapping("/skill-gap")
    public ResponseEntity<SkillGapResponse> analyzeSkillGap(@RequestParam Long candidateId,
                                                            @RequestParam Long jobId) {
        return ResponseEntity.ok(skillGapService.analyzeSkillGap(candidateId, jobId));
    }

    @PostMapping("/applications/{applicationId}/video-interview")
    public ResponseEntity<VideoInterviewResponse> scheduleVideoInterview(@PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewService.scheduleVideoInterview(applicationId));
    }
}
