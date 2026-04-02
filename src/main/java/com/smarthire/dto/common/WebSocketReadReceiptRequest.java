package com.smarthire.dto.common;

import jakarta.validation.constraints.NotNull;

public record WebSocketReadReceiptRequest(
        @NotNull(message = "Chat room id is required")
        Long chatRoomId
) {
}
