package com.smarthire.repository;

import java.util.List;

import com.smarthire.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findAllByChatIdOrderByCreatedAtAsc(Long chatId);
}
