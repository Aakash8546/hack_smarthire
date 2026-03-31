package com.smarthire.dto.common;

import org.springframework.core.io.Resource;

public record ResumeDownloadResponse(
        Resource resource,
        String fileName
) {
}
