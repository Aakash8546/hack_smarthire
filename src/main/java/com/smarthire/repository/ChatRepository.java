package com.smarthire.repository;

import java.util.Optional;

import com.smarthire.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByApplicationId(Long applicationId);
}
