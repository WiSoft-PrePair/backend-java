package io.wisoft.prepair.prepair_api.interview.session.service;

import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.interview.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionPersistenceService {

    private final SessionRepository sessionRepository;

    @Transactional
    public void saveCompletedSession(InterviewSession session, int finalScore, String finalFeedback) {
        session.complete(finalScore, finalFeedback);
        sessionRepository.save(session);
    }

    @Transactional
    public void saveFailedSession(InterviewSession session) {
        session.fail();
        sessionRepository.save(session);
    }
}
