package io.wisoft.prepair.prepair_api.interview.session.service;

import io.wisoft.prepair.prepair_api.common.exception.BusinessException;
import io.wisoft.prepair.prepair_api.common.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.interview.answer.entity.FeedbackType;
import io.wisoft.prepair.prepair_api.interview.answer.entity.InterviewAnswer;
import io.wisoft.prepair.prepair_api.interview.answer.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.interview.answer.repository.AnswerRepository;
import io.wisoft.prepair.prepair_api.interview.answer.repository.FeedbackRepository;
import io.wisoft.prepair.prepair_api.interview.question.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.interview.question.repository.QuestionRepository;
import io.wisoft.prepair.prepair_api.interview.session.dto.response.CreateSessionResponse;
import io.wisoft.prepair.prepair_api.interview.session.dto.response.SessionDetailResponse;
import io.wisoft.prepair.prepair_api.interview.session.dto.response.SessionResponse;
import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.interview.session.entity.SessionStatus;
import io.wisoft.prepair.prepair_api.interview.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;

    public List<SessionResponse> getSessionsByMember(UUID memberId) {
        return sessionRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(SessionResponse::from)
                .toList();
    }

    public SessionDetailResponse getSessionDetail(UUID sessionId, UUID memberId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        List<SessionDetailResponse.QuestionFeedback> questions = buildQuestionFeedbacks(sessionId);

        return new SessionDetailResponse(
                sessionId,
                session.getStatus(),
                session.getFinalScore(),
                session.getFinalFeedback(),
                questions
        );
    }

    public List<SessionDetailResponse.QuestionFeedback> buildQuestionFeedbacks(UUID sessionId) {
        List<InterviewQuestion> questions = questionRepository.findByInterviewSessionIdOrderByCreatedAtAsc(sessionId);
        Map<UUID, InterviewAnswer> answerMap = getAnswerMap(sessionId);
        Map<UUID, List<InterviewFeedback>> feedbackMap = getFeedbackMap(sessionId);

        List<SessionDetailResponse.QuestionFeedback> result = new ArrayList<>();
        for (InterviewQuestion question : questions) {
            InterviewAnswer answer = answerMap.get(question.getId());
            if (answer == null) {
                result.add(new SessionDetailResponse.QuestionFeedback(
                        question.getId(), question.getQuestion(),
                        null, null, null, null, null
                ));
                continue;
            }

            List<InterviewFeedback> answerFeedbacks = feedbackMap.getOrDefault(answer.getId(), List.of());
            InterviewFeedback combined = findFeedback(answerFeedbacks, FeedbackType.COMBINED);

            result.add(new SessionDetailResponse.QuestionFeedback(
                    question.getId(),
                    question.getQuestion(),
                    answer.getMediaUrl(),
                    combined == null ? null : combined.getScore(),
                    combined == null ? null : combined.getFeedback(),
                    getFeedbackText(answerFeedbacks, FeedbackType.STT),
                    getFeedbackText(answerFeedbacks, FeedbackType.VIDEO)
            ));
        }
        return result;
    }

    @Transactional
    public CreateSessionResponse createSession(UUID oldSessionId, UUID memberId) {
        InterviewSession oldSession = sessionRepository.findById(oldSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (!oldSession.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (oldSession.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FINISHED);
        }

        List<InterviewQuestion> oldQuestions = questionRepository.findByInterviewSessionIdOrderByCreatedAtAsc(oldSessionId);
        InterviewSession newSession = sessionRepository.save(new InterviewSession(memberId, oldQuestions.size()));

        List<UUID> newQuestionIds = oldQuestions.stream()
                .map(q -> cloneQuestion(q, newSession).getId())
                .toList();

        return new CreateSessionResponse(newSession.getId(), newQuestionIds);
    }

    @Transactional
    public CreateSessionResponse createQuestion(UUID oldSessionId, UUID oldQuestionId, UUID memberId) {
        InterviewSession oldSession = sessionRepository.findById(oldSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (!oldSession.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (oldSession.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FINISHED);
        }

        InterviewQuestion oldQuestion = questionRepository.findByIdAndMemberIdAndInterviewSessionId(
                        oldQuestionId,
                        memberId,
                        oldSessionId
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        InterviewSession newSession = sessionRepository.save(new InterviewSession(memberId, 1));
        InterviewQuestion newQuestion = cloneQuestion(oldQuestion, newSession);

        return new CreateSessionResponse(newSession.getId(), List.of(newQuestion.getId()));
    }

    private InterviewQuestion cloneQuestion(InterviewQuestion source, InterviewSession newSession) {
        return questionRepository.save(new InterviewQuestion(
                source.getMemberId(),
                source.getQuestion(),
                source.getQuestionType(),
                source.getQuestionTag(),
                source.getJobPosting(),
                newSession
        ));
    }

    @Transactional
    public void saveCompletedSession(UUID sessionId, int finalScore, String finalFeedback) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() == SessionStatus.COMPLETED) {
            return;
        }

        session.complete(finalScore, finalFeedback);
    }

    @Transactional
    public void saveFailedSession(UUID sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            return;
        }

        session.fail();
    }

    private Map<UUID, InterviewAnswer> getAnswerMap(UUID sessionId) {
        return answerRepository.findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(
                        a -> a.getInterviewQuestion().getId(),
                        a -> a,
                        (existing, replacement) ->
                                existing.getCreatedAt().isAfter(replacement.getCreatedAt()) ? existing : replacement
                ));
    }

    private Map<UUID, List<InterviewFeedback>> getFeedbackMap(UUID sessionId) {
        return feedbackRepository.findAllBySessionId(sessionId).stream()
                .collect(Collectors.groupingBy(f -> f.getInterviewAnswer().getId()));
    }

    private InterviewFeedback findFeedback(List<InterviewFeedback> feedbacks, FeedbackType type) {
        return feedbacks.stream()
                .filter(f -> f.getFeedbackType() == type)
                .findFirst()
                .orElse(null);
    }

    private String getFeedbackText(List<InterviewFeedback> feedbacks, FeedbackType type) {
        InterviewFeedback feedback = findFeedback(feedbacks, type);
        return feedback == null ? null : feedback.getFeedback();
    }
}
