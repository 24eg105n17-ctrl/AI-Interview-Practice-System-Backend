package com.aiinterview.model;

public record EvaluationResponse(
        int score,
        String strengths,
        String weaknesses,
        String suggestions,
        String modelAnswer,
        String mode
) {
}
