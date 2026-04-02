package com.smarthire.dto.common;

import java.time.OffsetDateTime;

public record ChatRoomResponse(
        Long chatRoomId,
        Long applicationId,
        Long jobId,
        String jobTitle,
        Long candidateId,
        String candidateName,
        Long recruiterId,
        String recruiterName,
        OffsetDateTime createdAt
) {
}
