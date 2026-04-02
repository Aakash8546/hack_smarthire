package com.smarthire.service.impl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import com.smarthire.dto.common.ChatMessageRequest;
import com.smarthire.dto.common.ChatMessageResponse;
import com.smarthire.dto.common.ChatReadReceiptResponse;
import com.smarthire.dto.common.ChatRoomResponse;
import com.smarthire.dto.common.WebSocketChatMessageRequest;
import com.smarthire.dto.common.WebSocketReadReceiptRequest;
import com.smarthire.entity.Chat;
import com.smarthire.entity.Job;
import com.smarthire.entity.JobApplication;
import com.smarthire.entity.Message;
import com.smarthire.entity.User;
import com.smarthire.entity.enums.MessageStatus;
import com.smarthire.entity.enums.UserRole;
import com.smarthire.exception.BadRequestException;
import com.smarthire.exception.ResourceNotFoundException;
import com.smarthire.repository.ChatRepository;
import com.smarthire.repository.JobApplicationRepository;
import com.smarthire.repository.JobRepository;
import com.smarthire.repository.MessageRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.service.ChatService;
import com.smarthire.service.EmailService;
import com.smarthire.service.ml.MlIntegrationService;
import com.smarthire.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final MlIntegrationService mlIntegrationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    @Override
    @Transactional
    public ChatRoomResponse initChat(Long jobId) {
        User currentUser = getCurrentUser();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        JobApplication application = resolveApplicationForChatInit(currentUser, job);
        Chat chat = chatRepository.findByApplication(application).orElseGet(() -> createChat(application));
        return toChatRoomResponse(chat);
    }

    @Override
    @Transactional
    public List<ChatMessageResponse> getMessagesByChatRoom(Long chatRoomId) {
        User currentUser = getCurrentUser();
        Chat chat = getAuthorizedChatByRoomId(chatRoomId, currentUser);
        updateReadState(chat, currentUser);
        return messageRepository.findAllByChatIdOrderByCreatedAtAsc(chat.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long applicationId) {
        User currentUser = getCurrentUser();
        Chat chat = getAuthorizedChat(applicationId, currentUser);
        return getMessagesByChatRoom(chat.getId());
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long applicationId, ChatMessageRequest request) {
        User currentUser = getCurrentUser();
        Chat chat = getAuthorizedChat(applicationId, currentUser);
        return persistAndDispatchMessage(chat, currentUser, request.content(), false);
    }

    @Override
    @Transactional
    public ChatMessageResponse sendRealtimeMessage(WebSocketChatMessageRequest request, Principal principal) {
        if (principal == null) {
            throw new BadRequestException("WebSocket user is not authenticated");
        }
        User currentUser = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Chat chat = getAuthorizedChatByRoomId(request.chatRoomId(), currentUser);
        return persistAndDispatchMessage(chat, currentUser, request.content(), true);
    }

    @Override
    @Transactional
    public ChatReadReceiptResponse markMessagesAsRead(Long chatRoomId) {
        User currentUser = getCurrentUser();
        Chat chat = getAuthorizedChatByRoomId(chatRoomId, currentUser);
        return updateReadState(chat, currentUser);
    }

    @Override
    @Transactional
    public ChatReadReceiptResponse updateReadReceipt(WebSocketReadReceiptRequest request, Principal principal) {
        if (principal == null) {
            throw new BadRequestException("WebSocket user is not authenticated");
        }
        User currentUser = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Chat chat = getAuthorizedChatByRoomId(request.chatRoomId(), currentUser);
        return updateReadState(chat, currentUser);
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

    private Chat getAuthorizedChatByRoomId(Long chatRoomId, User currentUser) {
        Chat chat = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
        boolean authorized = chat.getCandidate().getId().equals(currentUser.getId()) || chat.getRecruiter().getId().equals(currentUser.getId());
        if (!authorized) {
            throw new BadRequestException("You are not authorized to access this chat");
        }
        return chat;
    }

    private JobApplication resolveApplicationForChatInit(User currentUser, Job job) {
        if (currentUser.getRole() == UserRole.CANDIDATE) {
            return jobApplicationRepository.findByCandidateAndJob(currentUser, job)
                    .orElseThrow(() -> new BadRequestException("Apply to this job before starting a chat"));
        }
        if (currentUser.getRole() == UserRole.RECRUITER) {
            throw new BadRequestException("Recruiter should open chat using the candidate application context");
        }
        throw new BadRequestException("Unsupported user role for chat");
    }

    private Chat createChat(JobApplication application) {
        Chat chat = new Chat();
        chat.setApplication(application);
        chat.setCandidate(application.getCandidate());
        chat.setRecruiter(application.getJob().getRecruiter());
        return chatRepository.save(chat);
    }

    private ChatReadReceiptResponse updateReadState(Chat chat, User currentUser) {
        List<Message> unreadMessages = messageRepository.findAllByChatIdAndReceiverAndStatusIn(
                chat.getId(), currentUser, List.of(MessageStatus.SENT, MessageStatus.DELIVERED)
        );
        if (unreadMessages.isEmpty()) {
            return new ChatReadReceiptResponse(
                    chat.getId(),
                    currentUser.getId(),
                    List.of(),
                    MessageStatus.READ.name(),
                    java.time.OffsetDateTime.now()
            );
        }
        unreadMessages.forEach(message -> message.setStatus(MessageStatus.READ));
        List<Message> savedMessages = messageRepository.saveAll(unreadMessages);
        ChatReadReceiptResponse receipt = new ChatReadReceiptResponse(
                chat.getId(),
                currentUser.getId(),
                savedMessages.stream().map(Message::getId).toList(),
                MessageStatus.READ.name(),
                java.time.OffsetDateTime.now()
        );
        User otherParticipant = chat.getCandidate().getId().equals(currentUser.getId()) ? chat.getRecruiter() : chat.getCandidate();
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getId() + "/read-receipts", receipt);
        messagingTemplate.convertAndSendToUser(otherParticipant.getEmail(), "/queue/read-receipts", receipt);
        log.info("Marked {} messages as READ in room {} for user {}", savedMessages.size(), chat.getId(), currentUser.getEmail());
        return receipt;
    }

    private ChatMessageResponse persistAndDispatchMessage(Chat chat, User sender, String content, boolean websocketOriginated) {
        User receiver = chat.getCandidate().getId().equals(sender.getId()) ? chat.getRecruiter() : chat.getCandidate();
        var moderationResult = mlIntegrationService.detectSpam(content);
        boolean spam = moderationResult.spam();
        String sanitizedContent = moderationResult.sanitizedContent();

        Message message = new Message();
        message.setChat(chat);
        message.setJob(chat.getApplication().getJob());
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(sanitizedContent);
        message.setSpam(spam);
        message.setStatus(MessageStatus.SENT);
        Message savedMessage = messageRepository.save(message);

        savedMessage.setStatus(MessageStatus.DELIVERED);
        savedMessage = messageRepository.save(savedMessage);

        ChatMessageResponse response = toResponse(savedMessage);
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getId(), response);
        messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/chat-notifications", response);

        if (!spam) {
            emailService.sendChatMessageNotificationEmail(
                    receiver.getEmail(),
                    receiver.getName(),
                    sender.getName(),
                    chat.getApplication().getJob().getTitle(),
                    sanitizedContent
            );
        }
        log.info("Chat message sent in room {} from {} to {} via {}. label={}, spam={}, abusive={}",
                chat.getId(), sender.getEmail(), receiver.getEmail(),
                websocketOriginated ? "websocket" : "rest",
                moderationResult.label(), moderationResult.spam(), moderationResult.abusive());
        return response;
    }

    private ChatRoomResponse toChatRoomResponse(Chat chat) {
        return new ChatRoomResponse(
                chat.getId(),
                chat.getApplication().getId(),
                chat.getApplication().getJob().getId(),
                chat.getApplication().getJob().getTitle(),
                chat.getCandidate().getId(),
                chat.getCandidate().getName(),
                chat.getRecruiter().getId(),
                chat.getRecruiter().getName(),
                chat.getCreatedAt()
        );
    }

    private ChatMessageResponse toResponse(Message message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChat().getId(),
                message.getJob().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getReceiver().getId(),
                message.getContent(),
                message.isSpam(),
                message.getStatus().name(),
                message.getCreatedAt()
        );
    }
}
