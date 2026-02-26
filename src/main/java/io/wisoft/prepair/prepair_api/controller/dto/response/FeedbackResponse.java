package io.wisoft.prepair.prepair_api.controller.dto.response;

import io.wisoft.prepair.prepair_api.controller.dto.FeedbackDetail;
import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        String answer,
        FeedbackDetail feedback,
        Integer score,
        LocalDateTime createAt
) {

    public static FeedbackResponse from(InterviewFeedback f, FeedbackDetail detail) {
        return new FeedbackResponse(
                f.getId(),
                f.getInterviewAnswer().getAnswer(),
                detail,
                f.getScore(),
                f.getCreatedAt()
        );
    }
}
