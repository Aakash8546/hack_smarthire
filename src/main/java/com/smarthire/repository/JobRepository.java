package com.smarthire.repository;

import java.util.List;

import com.smarthire.entity.Job;
import com.smarthire.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findAllByRecruiterOrderByCreatedAtDesc(User recruiter);
}
