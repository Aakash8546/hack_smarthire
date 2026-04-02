package com.smarthire.repository;

import java.util.Optional;

import com.smarthire.entity.Chat;
import com.smarthire.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByApplicationId(Long applicationId);

    Optional<Chat> findByApplication(JobApplication application);
}
