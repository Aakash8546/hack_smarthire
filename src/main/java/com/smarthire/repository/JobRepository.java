package com.smarthire.repository;

import java.util.List;
import java.util.Optional;

import com.smarthire.entity.Job;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findAllByRecruiterOrderByCreatedAtDesc(User recruiter);

    List<Job> findAllByStatusOrderByCreatedAtDesc(JobStatus status);

    Optional<Job> findByIdAndRecruiter(Long id, User recruiter);
}
