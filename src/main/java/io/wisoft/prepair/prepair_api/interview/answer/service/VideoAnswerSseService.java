package io.wisoft.prepair.prepair_api.interview.answer.service;

import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.common.exception.BusinessException;
import io.wisoft.prepair.prepair_api.common.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.common.support.SseEmitterManager;
import io.wisoft.prepair.prepair_api.interview.session.repository.SessionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class VideoAnswerSseService {

    private final SessionRepository sessionRepository;
    private final SseEmitterManager sseEmitterManager;

    public SseEmitter subscribe(final UUID sessionId, final UUID memberId) {
        validateSessionOwner(sessionId, memberId);
        return sseEmitterManager.create(sessionId);
    }

    private void validateSessionOwner(final UUID sessionId, final UUID memberId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
