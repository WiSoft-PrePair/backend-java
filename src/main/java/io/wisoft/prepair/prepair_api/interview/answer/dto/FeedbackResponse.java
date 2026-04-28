package io.wisoft.prepair.prepair_api.interview.answer.dto;

import io.wisoft.prepair.prepair_api.interview.answer.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.interview.answer.entity.FeedbackType;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        String answer,
        FeedbackDetail feedback,
        FeedbackType feedbackType,
        Integer score,
        LocalDateTime createdAt
) {

    public static FeedbackResponse from(InterviewFeedback f, FeedbackDetail detail) {
        return new FeedbackResponse(
                f.getId(),
                f.getInterviewAnswer().getAnswer(),
                detail,
                f.getFeedbackType(),
                f.getScore(),
                f.getCreatedAt()
        );
    }
}
