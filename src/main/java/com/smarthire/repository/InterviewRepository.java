package com.smarthire.repository;

import java.util.List;
import java.util.Optional;

import com.smarthire.entity.Interview;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.InterviewType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findAllByCandidateAndTypeOrderByCreatedAtDesc(User candidate, InterviewType type);

    Optional<Interview> findByIdAndCandidateAndType(Long id, User candidate, InterviewType type);
}
