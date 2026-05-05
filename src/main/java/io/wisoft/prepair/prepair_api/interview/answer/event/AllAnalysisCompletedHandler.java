package io.wisoft.prepair.prepair_api.interview.answer.event;

import io.wisoft.prepair.prepair_api.interview.answer.dto.CombinedFeedbackResult;
import io.wisoft.prepair.prepair_api.interview.answer.dto.FinalFeedbackResult;
import io.wisoft.prepair.prepair_api.interview.answer.dto.FinalFeedbackResponse;
import io.wisoft.prepair.prepair_api.interview.answer.entity.InterviewAnswer;
import io.wisoft.prepair.prepair_api.interview.answer.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.interview.answer.entity.FeedbackType;
import io.wisoft.prepair.prepair_api.interview.answer.repository.AnswerRepository;
import io.wisoft.prepair.prepair_api.interview.answer.repository.FeedbackRepository;
import io.wisoft.prepair.prepair_api.interview.answer.service.AnswerPersistenceService;
import io.wisoft.prepair.prepair_api.interview.answer.service.FeedbackGenerator;
import io.wisoft.prepair.prepair_api.interview.question.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.interview.question.repository.QuestionRepository;
import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.interview.session.service.SessionPersistenceService;
import io.wisoft.prepair.prepair_api.common.support.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllAnalysisCompletedHandler {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackGenerator feedbackGenerator;
    private final AnswerPersistenceService answerPersistenceService;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final SessionPersistenceService sessionPersistenceService;
    private final SseEmitterManager sseEmitterManager;

    @Async("videoTaskExecutor")
    @EventListener
    public void handle(AllAnalysisCompletedEvent event) {
        UUID answerId = event.answerId();
        deleteTempFile(event.videoPath());

        if (event.hasFailed()) {
            log.error("[종합평가] 분석 실패로 종합평가 생략 - answerId: {}", answerId);
            failSession(answerId, "분석 중 오류가 발생했습니다.");
            return;
        }

        try {
            Optional<AnalysisFeedbacks> feedbacksOpt = findAnalysisFeedbacks(answerId);
            if (feedbacksOpt.isEmpty()) {
                failSession(answerId, "분석 결과가 누락되어 종합 평가를 생성할 수 없습니다.");
                return;
            }

            InterviewAnswer answer = answerRepository.findByIdWithQuestionAndSession(answerId).orElse(null);
            if (answer == null) return;

            saveCombinedFeedback(answerId, answer, feedbacksOpt.get());
            tryGenerateFinalFeedback(answer);

        } catch (Exception e) {
            log.error("[종합평가] 실패 - answerId: {}, error: {}", answerId, e.getMessage(), e);
            failSession(answerId, "종합 평가 생성 중 오류가 발생했습니다.");
        }
    }

    private Optional<AnalysisFeedbacks> findAnalysisFeedbacks(UUID answerId) {
        List<InterviewFeedback> feedbacks = feedbackRepository.findByInterviewAnswerId(answerId);

        InterviewFeedback stt = feedbacks.stream()
                .filter(f -> f.getFeedbackType() == FeedbackType.STT)
                .findFirst()
                .orElse(null);

        InterviewFeedback video = feedbacks.stream()
                .filter(f -> f.getFeedbackType() == FeedbackType.VIDEO)
                .findFirst()
                .orElse(null);

        if (stt == null || video == null) {
            log.error("[종합평가] STT 또는 Video 피드백 없음 - answerId: {}", answerId);
            return Optional.empty();
        }

        return Optional.of(new AnalysisFeedbacks(stt, video));
    }

    private void saveCombinedFeedback(UUID answerId, InterviewAnswer answer, AnalysisFeedbacks feedbacks) {
        String question = answer.getInterviewQuestion().getQuestion();

        CombinedFeedbackResult result = feedbackGenerator.generateCombined(
                question,
                feedbacks.stt().getFeedback(),
                feedbacks.video().getFeedback()
        );

        answerPersistenceService.saveCombinedFeedback(answerId, result);
        log.info("[종합평가] 완료 - answerId: {}, score: {}", answerId, result.score());
    }

    private void tryGenerateFinalFeedback(InterviewAnswer answer) {
        InterviewSession session = answer.getInterviewQuestion().getInterviewSession();
        if (session == null) {
            log.warn("[최종평가] 세션 없음 - answerId: {}", answer.getId());
            return;
        }

        UUID sessionId = session.getId();
        if (!isFinalFeedbackReady(sessionId, session.getTotalQuestionCount())) return;

        List<InterviewQuestion> questions = questionRepository.findByInterviewSessionId(sessionId);

        Map<UUID, InterviewAnswer> answerMap = answerRepository.findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(a -> a.getInterviewQuestion().getId(), a -> a));

        Map<UUID, List<InterviewFeedback>> feedbackMap = feedbackRepository.findAllBySessionId(sessionId).stream()
                .collect(Collectors.groupingBy(f -> f.getInterviewAnswer().getId()));

        FinalFeedbackData data = buildFinalData(questions, answerMap, feedbackMap);
        FinalFeedbackResult finalResult = feedbackGenerator.generateFinal(data.promptInput());

        completeSession(session, data, finalResult);
    }

    private boolean isFinalFeedbackReady(UUID sessionId, int totalQuestionCount) {
        long combinedCount = feedbackRepository.countBySessionIdAndFeedbackType(sessionId, FeedbackType.COMBINED);

        if (combinedCount < totalQuestionCount) {
            log.info("[최종평가] 아직 모든 질문 완료되지 않음 - sessionId: {}, {}/{}", sessionId, combinedCount, totalQuestionCount);
            return false;
        }
        return true;
    }

    private FinalFeedbackData buildFinalData(
            List<InterviewQuestion> questions,
            Map<UUID, InterviewAnswer> answerMap,
            Map<UUID, List<InterviewFeedback>> feedbackMap
    ) {
        StringBuilder promptInput = new StringBuilder();
        List<FinalFeedbackResponse.QuestionFeedback> questionFeedbacks = new ArrayList<>();
        int totalScore = 0;

        for (InterviewQuestion q : questions) {
            InterviewAnswer ans = answerMap.get(q.getId());
            if (ans == null) continue;

            List<InterviewFeedback> answerFeedbacks = feedbackMap.getOrDefault(ans.getId(), List.of());

            InterviewFeedback combined = answerFeedbacks.stream()
                    .filter(f -> f.getFeedbackType() == FeedbackType.COMBINED)
                    .findFirst()
                    .orElse(null);
            if (combined == null) continue;

            String sttFeedbackStr = answerFeedbacks.stream()
                    .filter(f -> f.getFeedbackType() == FeedbackType.STT)
                    .findFirst()
                    .map(InterviewFeedback::getFeedback)
                    .orElse(null);

            String videoFeedbackStr = answerFeedbacks.stream()
                    .filter(f -> f.getFeedbackType() == FeedbackType.VIDEO)
                    .findFirst()
                    .map(InterviewFeedback::getFeedback)
                    .orElse(null);

            promptInput.append("질문: ").append(q.getQuestion()).append("\n");
            promptInput.append("종합 평가: ").append(combined.getFeedback()).append("\n");
            promptInput.append("점수: ").append(combined.getScore()).append("\n\n");

            questionFeedbacks.add(new FinalFeedbackResponse.QuestionFeedback(
                    q.getId(),
                    q.getQuestion(),
                    combined.getScore(),
                    combined.getFeedback(),
                    sttFeedbackStr,
                    videoFeedbackStr
            ));

            totalScore += combined.getScore();
        }

        int finalScore = questionFeedbacks.isEmpty() ? 0 : totalScore / questionFeedbacks.size();
        return new FinalFeedbackData(promptInput.toString(), questionFeedbacks, finalScore);
    }

    private void completeSession(InterviewSession session, FinalFeedbackData data, FinalFeedbackResult finalResult) {
        UUID sessionId = session.getId();

        sessionPersistenceService.saveCompletedSession(session, data.finalScore(), finalResult.finalFeedback());

        FinalFeedbackResponse response = new FinalFeedbackResponse(
                sessionId,
                data.finalScore(),
                finalResult.finalFeedback(),
                data.questionFeedbacks()
        );

        sseEmitterManager.send(sessionId, "final-complete", response);
        sseEmitterManager.complete(sessionId);

        log.info("[최종평가] 완료 - sessionId: {}, finalScore: {}", sessionId, data.finalScore());
    }

    private void failSession(UUID answerId, String message) {
        InterviewAnswer answer = answerRepository.findByIdWithQuestionAndSession(answerId).orElse(null);
        if (answer == null || answer.getInterviewQuestion().getInterviewSession() == null) return;

        InterviewSession session = answer.getInterviewQuestion().getInterviewSession();
        sessionPersistenceService.saveFailedSession(session);

        sseEmitterManager.send(session.getId(), "analysis-failed", Map.of("message", message));
        sseEmitterManager.complete(session.getId());
    }

    private void deleteTempFile(Path videoPath) {
        if (videoPath == null) return;
        try {
            Files.deleteIfExists(videoPath);
        } catch (IOException e) {
            log.warn("[임시파일] 삭제 실패 - path: {}", videoPath, e);
        }
    }

    private record AnalysisFeedbacks(
            InterviewFeedback stt,
            InterviewFeedback video
    ) {
    }

    private record FinalFeedbackData(
            String promptInput,
            List<FinalFeedbackResponse.QuestionFeedback> questionFeedbacks,
            int finalScore
    ) {
    }
}