package io.wisoft.prepair.prepair_api.controller.dto.response;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionStatus;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        String question,
        String questionTag,
        QuestionType questionType,
        QuestionStatus status,
        Integer latestScore,
        UUID jobPostingId,
        LocalDateTime createdAt
) {

    public static QuestionResponse from(InterviewQuestion question) {
        return new QuestionResponse(
                question.getId(),
                question.getQuestion(),
                question.getQuestionTag(),
                question.getQuestionType(),
                question.getStatus(),
                question.getLatestScore(),
                question.getJobPosting() != null ? question.getJobPosting().getId() : null,
                question.getCreatedAt()
        );
    }
}
