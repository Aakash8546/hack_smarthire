package com.smarthire.dto.candidate;

import java.util.List;

import com.smarthire.dto.common.JobResponse;

public record JobRecommendationResponse(
        List<JobResponse> recommendedJobs,
        String recommendationReason
) {
}
