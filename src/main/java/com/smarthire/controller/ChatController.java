package com.smarthire.controller;

import java.util.List;

import com.smarthire.dto.common.ChatMessageRequest;
import com.smarthire.dto.common.ChatMessageResponse;
import com.smarthire.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/application/{applicationId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@PathVariable Long applicationId) {
        return ResponseEntity.ok(chatService.getMessages(applicationId));
    }

    @PostMapping("/application/{applicationId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(@PathVariable Long applicationId,
                                                           @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(applicationId, request));
    }
}
