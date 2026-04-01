package com.smarthire.controller;

import com.smarthire.dto.candidate.InterviewCompletionResponse;
import com.smarthire.dto.candidate.InterviewEndSessionResponse;
import com.smarthire.dto.candidate.MockInterviewResultResponse;
import com.smarthire.dto.candidate.StartMockInterviewResponse;
import com.smarthire.dto.candidate.SubmitMockInterviewAnswerRequest;
import com.smarthire.dto.candidate.SubmitMockInterviewAnswerResponse;
import com.smarthire.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start-session")
    public ResponseEntity<StartMockInterviewResponse> startSession() {
        return ResponseEntity.ok(interviewService.startOrResumeCurrentSession());
    }

    @PostMapping("/answer")
    public ResponseEntity<SubmitMockInterviewAnswerResponse> submitAnswer(@Valid @RequestBody SubmitMockInterviewAnswerRequest request) {
        return ResponseEntity.ok(interviewService.submitCurrentSessionAnswer(request));
    }

    @GetMapping("/is-complete")
    public ResponseEntity<InterviewCompletionResponse> isComplete() {
        return ResponseEntity.ok(interviewService.isCurrentSessionComplete());
    }

    @GetMapping("/result")
    public ResponseEntity<MockInterviewResultResponse> getResult() {
        return ResponseEntity.ok(interviewService.getCurrentSessionResult());
    }

    @PostMapping("/end-session")
    public ResponseEntity<InterviewEndSessionResponse> endSession() {
        return ResponseEntity.ok(interviewService.endCurrentSession());
    }
}
