package io.wisoft.prepair.prepair_api.dto.response;

import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;

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
