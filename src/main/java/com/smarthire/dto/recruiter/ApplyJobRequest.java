package com.smarthire.dto.recruiter;

import jakarta.validation.constraints.Size;

public record ApplyJobRequest(
        @Size(max = 2000) String coverLetter
) {
}
