package com.smarthire.service;

import java.util.List;
import java.security.Principal;

import com.smarthire.dto.common.ChatMessageRequest;
import com.smarthire.dto.common.ChatMessageResponse;
import com.smarthire.dto.common.ChatReadReceiptResponse;
import com.smarthire.dto.common.ChatRoomResponse;
import com.smarthire.dto.common.WebSocketChatMessageRequest;
import com.smarthire.dto.common.WebSocketReadReceiptRequest;

public interface ChatService {

    ChatRoomResponse initChat(Long jobId);

    List<ChatMessageResponse> getMessagesByChatRoom(Long chatRoomId);

    List<ChatMessageResponse> getMessages(Long applicationId);

    ChatMessageResponse sendMessage(Long applicationId, ChatMessageRequest request);

    ChatMessageResponse sendRealtimeMessage(WebSocketChatMessageRequest request, Principal principal);

    ChatReadReceiptResponse markMessagesAsRead(Long chatRoomId);

    ChatReadReceiptResponse updateReadReceipt(WebSocketReadReceiptRequest request, Principal principal);
}
