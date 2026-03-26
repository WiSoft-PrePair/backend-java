package io.wisoft.prepair.prepair_api.dto;

public record FeedbackResult(
        String good,
        String improvement,
        String recommendation,
        Integer score
) {
}
