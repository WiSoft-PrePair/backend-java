package io.wisoft.prepair.prepair_api.interview.session.dto.response;

import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.interview.session.entity.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID sessionId,
        SessionStatus status,
        Integer finalScore,
        int totalQuestionCount,
        LocalDateTime createdAt
) {
    public static SessionResponse from(InterviewSession session) {
        return new SessionResponse(
                session.getId(),
                session.getStatus(),
                session.getFinalScore(),
                session.getTotalQuestionCount(),
                session.getCreatedAt()
        );
    }
}
