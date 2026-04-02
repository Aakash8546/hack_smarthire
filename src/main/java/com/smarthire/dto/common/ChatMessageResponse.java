package com.smarthire.dto.common;

import java.time.OffsetDateTime;

public record ChatMessageResponse(
        Long id,
        Long chatRoomId,
        Long jobId,
        Long senderId,
        String senderName,
        Long receiverId,
        String content,
        boolean spam,
        String status,
        OffsetDateTime createdAt
) {
}
