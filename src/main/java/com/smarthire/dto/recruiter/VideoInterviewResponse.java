package com.smarthire.dto.recruiter;

import java.time.OffsetDateTime;

public record VideoInterviewResponse(
        Long interviewId,
        Long applicationId,
        String provider,
        String meetingRoomId,
        String meetingUrl,
        String cheatingDetectionResult,
        OffsetDateTime createdAt
) {
}
