package com.smarthire.service;

import com.smarthire.dto.candidate.ResumeAnalysisResponse;
import com.smarthire.dto.candidate.ResumeExistsResponse;
import com.smarthire.dto.candidate.ResumeUploadResponse;
import com.smarthire.dto.common.ResumeDownloadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {

    ResumeUploadResponse uploadResume(MultipartFile file);

    String deleteResume();

    ResumeDownloadResponse downloadResume(Long candidateId);

    ResumeExistsResponse hasResume(Long candidateId);

    ResumeAnalysisResponse getResumeAnalysis(Long candidateId);
}
