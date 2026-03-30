package com.smarthire.dto.common;

import java.time.OffsetDateTime;

public record ChatMessageResponse(
        Long id,
        Long senderId,
        String senderName,
        String content,
        boolean spam,
        OffsetDateTime createdAt
) {
}
