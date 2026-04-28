package io.wisoft.prepair.prepair_api.dto;

import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;

public record AnswerSubmitResult(
        InterviewFeedback feedback,
        boolean firstAnswer
) {
}
