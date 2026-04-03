package com.smarthire.service;

import com.smarthire.dto.recruiter.SkillGapResponse;

public interface SkillGapService {

    SkillGapResponse analyzeSkillGap(Long jobId);
}
