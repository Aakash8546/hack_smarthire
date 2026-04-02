package com.smarthire.repository;

import java.util.List;
import java.util.Optional;

import com.smarthire.entity.Job;
import com.smarthire.entity.JobApplication;
import com.smarthire.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByCandidateAndJob(User candidate, Job job);

    List<JobApplication> findAllByCandidateOrderByCreatedAtDesc(User candidate);

    List<JobApplication> findAllByJobRecruiterOrderByCreatedAtDesc(User recruiter);

    List<JobApplication> findAllByJobAndJobRecruiterOrderByCreatedAtDesc(Job job, User recruiter);

    Optional<JobApplication> findByIdAndJobRecruiter(Long id, User recruiter);

    boolean existsByCandidateAndJobRecruiter(User candidate, User recruiter);
}
