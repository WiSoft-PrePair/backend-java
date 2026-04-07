package io.wisoft.prepair.prepair_api.dto.response;

import java.util.List;
import java.util.UUID;

public record FinalFeedbackResponse(
        UUID sessionId,
        int finalScore,
        String summary,
        List<QuestionFeedback> questions
) {
    public record QuestionFeedback(
            UUID questionId,
            String question,
            int combinedScore,
            String combinedFeedback,
            String sttFeedback,
            String videoFeedback
    ) {
    }
}