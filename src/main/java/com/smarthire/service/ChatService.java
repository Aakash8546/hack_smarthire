package com.smarthire.service;

import java.util.List;

import com.smarthire.dto.common.ChatMessageRequest;
import com.smarthire.dto.common.ChatMessageResponse;

public interface ChatService {

    List<ChatMessageResponse> getMessages(Long applicationId);

    ChatMessageResponse sendMessage(Long applicationId, ChatMessageRequest request);
}
