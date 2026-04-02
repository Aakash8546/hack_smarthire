package com.smarthire.dto.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WebSocketChatMessageRequest(
        @NotNull Long chatRoomId,
        @NotBlank @Size(max = 4000) String content
) {
}
