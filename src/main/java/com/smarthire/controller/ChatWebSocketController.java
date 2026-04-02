package com.smarthire.controller;

import java.security.Principal;

import com.smarthire.dto.common.ChatMessageResponse;
import com.smarthire.dto.common.ChatReadReceiptResponse;
import com.smarthire.dto.common.WebSocketChatMessageRequest;
import com.smarthire.dto.common.WebSocketReadReceiptRequest;
import com.smarthire.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    @SendToUser("/queue/errors")
    public ChatMessageResponse sendMessage(@Valid @Payload WebSocketChatMessageRequest request, Principal principal) {
        return chatService.sendRealtimeMessage(request, principal);
    }

    @MessageMapping("/chat.markRead")
    @SendToUser("/queue/errors")
    public ChatReadReceiptResponse markRead(@Valid @Payload WebSocketReadReceiptRequest request, Principal principal) {
        return chatService.updateReadReceipt(request, principal);
    }
}
