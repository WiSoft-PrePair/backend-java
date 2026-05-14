package io.wisoft.prepair.prepair_api.interview.answer.dto.internal;

import io.wisoft.prepair.prepair_api.interview.answer.entity.InterviewFeedback;

public record AnswerSubmitResult(
        InterviewFeedback feedback,
        boolean firstAnswer
) {
}
