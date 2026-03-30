package com.smarthire.service;

import java.util.List;

import com.smarthire.dto.common.ApplicationResponse;
import com.smarthire.dto.recruiter.ApplyJobRequest;
import com.smarthire.dto.recruiter.UpdateApplicationStatusRequest;

public interface ApplicationService {

    ApplicationResponse applyToJob(Long jobId, ApplyJobRequest request);

    List<ApplicationResponse> getMyApplications();

    List<ApplicationResponse> getRecruiterApplications();

    ApplicationResponse updateStatus(Long applicationId, UpdateApplicationStatusRequest request);
}
