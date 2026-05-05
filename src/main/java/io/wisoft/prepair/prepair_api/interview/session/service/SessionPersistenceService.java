package io.wisoft.prepair.prepair_api.interview.session.service;

import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.interview.session.entity.SessionStatus;
import io.wisoft.prepair.prepair_api.interview.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionPersistenceService {

    private final SessionRepository sessionRepository;

    @Transactional
    public void saveCompletedSession(UUID sessionId, int finalScore, String finalFeedback) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세션입니다."));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            return;
        }

        session.complete(finalScore, finalFeedback);
    }

    @Transactional
    public void saveFailedSession(UUID sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 세션입니다."));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            return;
        }

        session.fail();
    }
}
