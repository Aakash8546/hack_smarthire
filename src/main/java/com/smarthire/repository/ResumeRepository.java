package com.smarthire.repository;

import java.util.Optional;

import com.smarthire.entity.Resume;
import com.smarthire.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByCandidate(User candidate);

    void deleteByCandidate(User candidate);
}
