package com.smarthire.service.impl;

import java.util.List;

import com.smarthire.dto.common.ChatMessageRequest;
import com.smarthire.dto.common.ChatMessageResponse;
import com.smarthire.entity.Chat;
import com.smarthire.entity.Message;
import com.smarthire.entity.User;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.ChatRepository;
import com.smarthire.repository.MessageRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.ChatService;
import com.smarthire.service.ml.MlIntegrationService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MlIntegrationService mlIntegrationService;

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long applicationId) {
        User currentUser = getCurrentUser();
        Chat chat = getAuthorizedChat(applicationId, currentUser);
        return messageRepository.findAllByChatIdOrderByCreatedAtAsc(chat.getId()).stream()
                .map(message -> new ChatMessageResponse(message.getId(), message.getSender().getId(), message.getSender().getName(),
                        message.getContent(), message.isSpam(), message.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long applicationId, ChatMessageRequest request) {
        User currentUser = getCurrentUser();
        Chat chat = getAuthorizedChat(applicationId, currentUser);
        boolean spam = mlIntegrationService.detectSpam(request.content()).spam();
        Message message = new Message();
        message.setChat(chat);
        message.setSender(currentUser);
        message.setContent(request.content());
        message.setSpam(spam);
        Message savedMessage = messageRepository.save(message);
        return new ChatMessageResponse(savedMessage.getId(), currentUser.getId(), currentUser.getName(),
                savedMessage.getContent(), savedMessage.isSpam(), savedMessage.getCreatedAt());
    }

    private User getCurrentUser() {
        return userRepository.findById(SecurityUtils.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Chat getAuthorizedChat(Long applicationId, User currentUser) {
        Chat chat = chatRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));
        boolean authorized = chat.getCandidate().getId().equals(currentUser.getId()) || chat.getRecruiter().getId().equals(currentUser.getId());
        if (!authorized) {
            throw new BadRequestException("You are not authorized to access this chat");
        }
        return chat;
    }
}
