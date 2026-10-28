package com.aiinterview.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InterviewRequest(
        @NotBlank @Size(max = 60) String topic,
        @Size(max = 30) String difficulty,
        @NotBlank @Size(max = 1200) String question,
        @NotBlank @Size(max = 12000) String answer
) {
}
