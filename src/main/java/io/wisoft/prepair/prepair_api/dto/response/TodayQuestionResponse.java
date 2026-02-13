package io.wisoft.prepair.prepair_api.dto.response;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionStatus;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record TodayQuestionResponse(
        UUID questionId,
        String question,
        QuestionType questionType,
        String questionTag,
        QuestionStatus status,
        Integer latestScore,
        String sourceRef,
        LocalDateTime createdAt
) {
    public static TodayQuestionResponse from(InterviewQuestion question) {
        return new TodayQuestionResponse(
                question.getId(),
                question.getQuestion(),
                question.getQuestionType(),
                question.getQuestionTag(),
                question.getStatus(),
                question.getLatestScore(),
                question.getSourceRef(),
                question.getCreatedAt()
        );
    }
}
