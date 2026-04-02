package com.smarthire.controller;

import java.util.List;

import com.smarthire.dto.common.ChatMessageResponse;
import com.smarthire.dto.common.ChatReadReceiptResponse;
import com.smarthire.dto.common.ChatRoomResponse;
import com.smarthire.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatService chatService;

    @PostMapping("/init/{jobId}")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('RECRUITER')")
    public ResponseEntity<ChatRoomResponse> initChat(@PathVariable Long jobId) {
        return ResponseEntity.ok(chatService.initChat(jobId));
    }

    @GetMapping("/{chatRoomId}/messages")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('RECRUITER')")
    public ResponseEntity<List<ChatMessageResponse>> getChatMessages(@PathVariable Long chatRoomId) {
        return ResponseEntity.ok(chatService.getMessagesByChatRoom(chatRoomId));
    }

    @PostMapping("/{chatRoomId}/read")
    @PreAuthorize("hasRole('CANDIDATE') or hasRole('RECRUITER')")
    public ResponseEntity<ChatReadReceiptResponse> markMessagesAsRead(@PathVariable Long chatRoomId) {
        return ResponseEntity.ok(chatService.markMessagesAsRead(chatRoomId));
    }
}
