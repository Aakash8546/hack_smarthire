package com.smarthire.repository;

import java.util.List;

import com.smarthire.entity.Interview;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.InterviewType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findAllByCandidateAndTypeOrderByCreatedAtDesc(User candidate, InterviewType type);
}
