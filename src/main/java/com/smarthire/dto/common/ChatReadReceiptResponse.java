package com.smarthire.dto.common;

import java.time.OffsetDateTime;
import java.util.List;

public record ChatReadReceiptResponse(
        Long chatRoomId,
        Long readerId,
        List<Long> messageIds,
        String status,
        OffsetDateTime updatedAt
) {
}
