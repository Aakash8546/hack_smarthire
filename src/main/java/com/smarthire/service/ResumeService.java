package com.smarthire.service;

import com.smarthire.dto.candidate.ResumeUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {

    ResumeUploadResponse uploadResume(MultipartFile file);

    String deleteResume();
}
