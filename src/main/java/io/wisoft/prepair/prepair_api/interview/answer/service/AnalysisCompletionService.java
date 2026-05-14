package io.wisoft.prepair.prepair_api.interview.answer.service;

import io.wisoft.prepair.prepair_api.common.exception.BusinessException;
import io.wisoft.prepair.prepair_api.common.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.interview.answer.dto.internal.CombinedFeedbackResult;
import io.wisoft.prepair.prepair_api.interview.answer.dto.internal.FinalFeedbackResult;
import io.wisoft.prepair.prepair_api.interview.answer.dto.response.FinalFeedbackResponse;
import io.wisoft.prepair.prepair_api.interview.answer.dto.internal.FinalFeedbackInput;
import io.wisoft.prepair.prepair_api.interview.answer.entity.FeedbackType;
import io.wisoft.prepair.prepair_api.interview.answer.entity.InterviewAnswer;
import io.wisoft.prepair.prepair_api.interview.answer.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.interview.answer.repository.AnswerRepository;
import io.wisoft.prepair.prepair_api.interview.answer.repository.FeedbackRepository;
import io.wisoft.prepair.prepair_api.interview.question.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.interview.question.repository.QuestionRepository;
import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.interview.session.notifier.SessionCompletionNotifier;
import io.wisoft.prepair.prepair_api.interview.session.service.SessionPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisCompletionService {

    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final FeedbackGenerator feedbackGenerator;
    private final AnswerPersistenceService answerPersistenceService;
    private final SessionPersistenceService sessionPersistenceService;
    private final SessionCompletionNotifier completionNotifier;

    public void processAnalysisCompletion(UUID answerId) {
        List<InterviewFeedback> feedbacks = feedbackRepository.findByInterviewAnswerId(answerId);

        InterviewFeedback stt = findRequiredFeedback(feedbacks, FeedbackType.STT);
        InterviewFeedback video = findRequiredFeedback(feedbacks, FeedbackType.VIDEO);

        InterviewAnswer answer = answerRepository.findByIdWithQuestionAndSession(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        CombinedFeedbackResult result = feedbackGenerator.generateCombined(
                answer.getInterviewQuestion().getQuestion(),
                stt.getFeedback(),
                video.getFeedback()
        );

        answerPersistenceService.saveCombinedFeedback(answerId, result);
        log.info("[종합평가] 완료 - answerId: {}, score: {}", answerId, result.score());
        completeSessionIfReady(answer);
    }

    private void completeSessionIfReady(InterviewAnswer answer) {
        InterviewSession session = getSession(answer);
        if (session == null) {
            log.warn("[최종평가] 세션 없음 - answerId: {}", answer.getId());
            return;
        }

        if (!isFinalFeedbackReady(session)) {
            return;
        }

        UUID sessionId = session.getId();
        FinalFeedbackData data = buildFinalData(sessionId);
        FinalFeedbackResult finalResult = feedbackGenerator.generateFinal(data.toInputs());

        completeSession(sessionId, data, finalResult);
    }


    public void failSession(UUID answerId, String message) {
        InterviewAnswer answer = answerRepository.findByIdWithQuestionAndSession(answerId).orElse(null);
        if (answer == null) {
            log.warn("[세션실패] 답변 없음 - answerId: {}", answerId);
            return;
        }

        InterviewSession session = getSession(answer);
        if (session == null) {
            log.warn("[세션실패] 세션 없음 - answerId: {}", answerId);
            return;
        }

        sessionPersistenceService.saveFailedSession(session.getId());
        completionNotifier.notifyFailure(session.getId(), message);
    }

    private InterviewSession getSession(InterviewAnswer answer) {
        return answer.getInterviewQuestion().getInterviewSession();
    }

    private InterviewFeedback findRequiredFeedback(List<InterviewFeedback> feedbacks, FeedbackType type) {
        return feedbacks.stream()
                .filter(f -> f.getFeedbackType() == type)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
    }

    private boolean isFinalFeedbackReady(InterviewSession session) {
        UUID sessionId = session.getId();
        long combinedCount = feedbackRepository.countBySessionIdAndFeedbackType(sessionId, FeedbackType.COMBINED);

        if (combinedCount < session.getTotalQuestionCount()) {
            log.info(
                    "[최종평가] 아직 모든 질문 완료되지 않음 - sessionId: {}, {}/{}",
                    sessionId,
                    combinedCount,
                    session.getTotalQuestionCount()
            );
            return false;
        }
        return true;
    }

    private void completeSession(
            UUID sessionId,
            FinalFeedbackData data,
            FinalFeedbackResult finalResult
    ) {
        sessionPersistenceService.saveCompletedSession(sessionId, data.finalScore(), finalResult.finalFeedback());

        FinalFeedbackResponse response = new FinalFeedbackResponse(
                sessionId,
                data.finalScore(),
                finalResult.finalFeedback(),
                data.questionFeedbacks()
        );

        completionNotifier.notifyComplete(sessionId, response);

        log.info("[최종평가] 완료 - sessionId: {}, finalScore: {}", sessionId, data.finalScore());
    }

    private FinalFeedbackData buildFinalData(UUID sessionId) {
        List<InterviewQuestion> questions = questionRepository.findByInterviewSessionId(sessionId);
        Map<UUID, InterviewAnswer> answerMap = getAnswerMap(sessionId);
        Map<UUID, List<InterviewFeedback>> feedbackMap = getFeedbackMap(sessionId);

        List<FinalFeedbackResponse.QuestionFeedback> questionFeedbacks = new ArrayList<>();
        int totalScore = 0;

        for (InterviewQuestion question : questions) {
            InterviewAnswer answer = answerMap.get(question.getId());
            if (answer == null) continue;

            List<InterviewFeedback> answerFeedbacks = feedbackMap.getOrDefault(answer.getId(), List.of());
            InterviewFeedback combined = findOptionalFeedback(answerFeedbacks, FeedbackType.COMBINED);

            if (combined == null) continue;

            questionFeedbacks.add(new FinalFeedbackResponse.QuestionFeedback(
                    question.getId(),
                    question.getQuestion(),
                    combined.getScore(),
                    combined.getFeedback(),
                    getFeedbackText(answerFeedbacks, FeedbackType.STT),
                    getFeedbackText(answerFeedbacks, FeedbackType.VIDEO)
            ));

            totalScore += combined.getScore();
        }

        int finalScore = calculateAverageScore(totalScore, questionFeedbacks.size());
        return new FinalFeedbackData(questionFeedbacks, finalScore);
    }

    private Map<UUID, InterviewAnswer> getAnswerMap(UUID sessionId) {
        return answerRepository.findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(a -> a.getInterviewQuestion().getId(), a -> a));
    }

    private Map<UUID, List<InterviewFeedback>> getFeedbackMap(UUID sessionId) {
        return feedbackRepository.findAllBySessionId(sessionId).stream()
                .collect(Collectors.groupingBy(f -> f.getInterviewAnswer().getId()));
    }

    private InterviewFeedback findOptionalFeedback(List<InterviewFeedback> feedbacks, FeedbackType type) {
        return feedbacks.stream()
                .filter(f -> f.getFeedbackType() == type)
                .findFirst()
                .orElse(null);
    }

    private String getFeedbackText(List<InterviewFeedback> feedbacks, FeedbackType type) {
        InterviewFeedback feedback = findOptionalFeedback(feedbacks, type);
        return feedback == null ? null : feedback.getFeedback();
    }

    private int calculateAverageScore(int totalScore, int count) {
        if (count == 0) {
            return 0;
        }

        return totalScore / count;
    }

    private record FinalFeedbackData(
            List<FinalFeedbackResponse.QuestionFeedback> questionFeedbacks,
            int finalScore
    ) {
        List<FinalFeedbackInput> toInputs() {
            return questionFeedbacks.stream()
                    .map(qf -> new FinalFeedbackInput(
                            qf.questionId(),
                            qf.question(),
                            qf.combinedFeedback(),
                            qf.combinedScore()
                    ))
                    .toList();
        }
    }
}
