package com.smarthire.service;

import java.util.List;

import com.smarthire.dto.candidate.JobRecommendationResponse;
import com.smarthire.dto.common.ApplicationResponse;
import com.smarthire.dto.common.JobResponse;
import com.smarthire.dto.recruiter.CreateJobRequest;

public interface JobService {

    JobResponse createJob(CreateJobRequest request);

    List<JobResponse> getAllJobs();

    List<JobResponse> getMyJobs();

    List<ApplicationResponse> getApplicationsForJob(Long jobId);

    JobResponse toggleJobStatus(Long jobId);

    JobRecommendationResponse recommendJobs();
}
