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
import io.wisoft.prepair.prepair_api.interview.session.dto.response.SessionDetailResponse;
import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.interview.session.notifier.SessionCompletionNotifier;
import io.wisoft.prepair.prepair_api.interview.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisCompletionService {

    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackGenerator feedbackGenerator;
    private final AnswerPersistenceService answerPersistenceService;
    private final SessionService sessionService;
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

        sessionService.saveFailedSession(session.getId());
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
        sessionService.saveCompletedSession(sessionId, data.finalScore(), finalResult.finalFeedback());

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
        List<SessionDetailResponse.QuestionFeedback> aggregated = sessionService.buildQuestionFeedbacks(sessionId);

        List<FinalFeedbackResponse.QuestionFeedback> questionFeedbacks = new ArrayList<>();
        int totalScore = 0;

        for (SessionDetailResponse.QuestionFeedback qf : aggregated) {
            if (qf.combinedScore() == null) continue;

            questionFeedbacks.add(new FinalFeedbackResponse.QuestionFeedback(
                    qf.questionId(),
                    qf.question(),
                    qf.combinedScore(),
                    qf.combinedFeedback(),
                    qf.sttFeedback(),
                    qf.videoFeedback()
            ));
            totalScore += qf.combinedScore();
        }

        int finalScore = calculateAverageScore(totalScore, questionFeedbacks.size());
        return new FinalFeedbackData(questionFeedbacks, finalScore);
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
