package com.smarthire.repository;

import java.util.List;

import com.smarthire.entity.SkillGapAnalysis;
import com.smarthire.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillGapAnalysisRepository extends JpaRepository<SkillGapAnalysis, Long> {

    List<SkillGapAnalysis> findAllByCandidateOrderByCreatedAtDesc(User candidate);
}
