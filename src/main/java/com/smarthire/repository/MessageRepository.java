package com.smarthire.repository;

import java.util.List;

import com.smarthire.entity.Message;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findAllByChatIdOrderByCreatedAtAsc(Long chatId);

    List<Message> findAllByChatIdAndReceiverAndStatusIn(Long chatId, User receiver, List<MessageStatus> statuses);

    List<Message> findAllByChatIdAndSenderAndStatus(Long chatId, User sender, MessageStatus status);
}
