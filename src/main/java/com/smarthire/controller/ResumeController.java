package com.smarthire.controller;

import com.smarthire.dto.candidate.ResumeAnalysisResponse;
import com.smarthire.dto.candidate.ResumeExistsResponse;
import com.smarthire.dto.common.ResumeDownloadResponse;
import com.smarthire.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping("/{resumeId}/download")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('RECRUITER')")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long resumeId) {
        ResumeDownloadResponse response = resumeService.downloadResume(resumeId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + response.fileName() + "\"")
                .body(response.resource());
    }

    @GetMapping("/{candidateId}/exists")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('RECRUITER')")
    public ResponseEntity<ResumeExistsResponse> hasResume(@PathVariable Long candidateId) {
        return ResponseEntity.ok(resumeService.hasResume(candidateId));
    }

    @GetMapping("/{candidateId}/analysis")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('RECRUITER')")
    public ResponseEntity<ResumeAnalysisResponse> getResumeAnalysis(@PathVariable Long candidateId) {
        return ResponseEntity.ok(resumeService.getResumeAnalysis(candidateId));
    }
}
