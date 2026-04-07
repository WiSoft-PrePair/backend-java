package io.wisoft.prepair.prepair_api.service.answer.event;

import io.wisoft.prepair.prepair_api.dto.CombinedFeedbackResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import io.wisoft.prepair.prepair_api.dto.FinalFeedbackResult;
import io.wisoft.prepair.prepair_api.dto.response.FinalFeedbackResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewAnswer;
import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;
import io.wisoft.prepair.prepair_api.repository.AnswerRepository;
import io.wisoft.prepair.prepair_api.repository.FeedbackRepository;
import io.wisoft.prepair.prepair_api.repository.QuestionRepository;
import io.wisoft.prepair.prepair_api.repository.SessionRepository;
import io.wisoft.prepair.prepair_api.service.answer.AnswerPersistService;
import io.wisoft.prepair.prepair_api.service.answer.FeedbackGenerator;
import io.wisoft.prepair.prepair_api.global.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllAnalysisCompletedHandler {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackGenerator feedbackGenerator;
    private final AnswerPersistService answerPersistService;
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final SessionRepository sessionRepository;
    private final SseEmitterManager sseEmitterManager;

    @Async("videoTaskExecutor")
    @EventListener
    @Transactional
    public void handle(AllAnalysisCompletedEvent event) {
        UUID answerId = event.answerId();

        deleteTempFile(event.videoPath());

        if (event.hasFailed()) {
            log.error("[종합평가] 분석 실패로 종합평가 생략 - answerId: {}", answerId);
            sendFailureToSession(answerId, "분석 중 오류가 발생했습니다.");
            return;
        }

        log.info("[종합평가] 생성 시작 - answerId: {}", answerId);

        try {
            List<InterviewFeedback> feedbacks = feedbackRepository.findByInterviewAnswerId(answerId);

            InterviewFeedback sttFeedback = feedbacks.stream()
                    .filter(f -> f.getFeedbackType() == FeedbackType.STT)
                    .findFirst().orElse(null);

            InterviewFeedback videoFeedback = feedbacks.stream()
                    .filter(f -> f.getFeedbackType() == FeedbackType.VIDEO)
                    .findFirst().orElse(null);

            if (sttFeedback == null || videoFeedback == null) {
                log.error("[종합평가] STT 또는 Video 피드백 없음 - answerId: {}", answerId);
                return;
            }

            InterviewAnswer answer = answerRepository.findByIdWithQuestionAndSession(answerId).orElse(null);
            if (answer == null) {
                return;
            }

            String question = answer.getInterviewQuestion().getQuestion();

            CombinedFeedbackResult result = feedbackGenerator.generateCombined(
                    question,
                    sttFeedback.getFeedback(),
                    videoFeedback.getFeedback()
            );

            answerPersistService.saveCombinedFeedback(answerId, result);
            log.info("[종합평가] 완료 - answerId: {}, score: {}", answerId, result.score());

            checkAndGenerateFinal(answer);
        } catch (Exception e) {
            log.error("[종합평가] 실패 - answerId: {}, error: {}", answerId, e.getMessage(), e);
            sendFailureToSession(answerId, "종합 평가 생성 중 오류가 발생했습니다.");
        }
    }

    private void checkAndGenerateFinal(InterviewAnswer answer) {
        InterviewSession session = answer.getInterviewQuestion().getInterviewSession();
        if (session == null) {
            log.warn("[최종평가] 세션 없음 - answerId: {}", answer.getId());
            return;
        }

        UUID sessionId = session.getId();
        long combinedCount = feedbackRepository.countBySessionIdAndFeedbackType(sessionId, FeedbackType.COMBINED);

        if (combinedCount < session.getTotalQuestionCount()) {
            log.info("[최종평가] 아직 모든 질문 완료되지 않음 - sessionId: {}, {}/{}", sessionId, combinedCount, session.getTotalQuestionCount());
            return;
        }

        log.info("[최종평가] 생성 시작 - sessionId: {}", sessionId);

        List<InterviewQuestion> questions = questionRepository.findByInterviewSessionId(sessionId);

        StringBuilder promptInput = new StringBuilder();
        List<FinalFeedbackResponse.QuestionFeedback> questionFeedbacks = new ArrayList<>();
        int totalScore = 0;

        for (InterviewQuestion q : questions) {
            InterviewAnswer ans = answerRepository.findByInterviewQuestionId(q.getId()).orElse(null);
            if (ans == null) continue;

            List<InterviewFeedback> answerFeedbacks = feedbackRepository.findByInterviewAnswerId(ans.getId());

            InterviewFeedback combined = answerFeedbacks.stream()
                    .filter(f -> f.getFeedbackType() == FeedbackType.COMBINED).findFirst().orElse(null);
            if (combined == null) continue;

            String sttFeedbackStr = answerFeedbacks.stream()
                    .filter(f -> f.getFeedbackType() == FeedbackType.STT).findFirst()
                    .map(InterviewFeedback::getFeedback).orElse(null);

            String videoFeedbackStr = answerFeedbacks.stream()
                    .filter(f -> f.getFeedbackType() == FeedbackType.VIDEO).findFirst()
                    .map(InterviewFeedback::getFeedback).orElse(null);

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

        FinalFeedbackResult finalResult = feedbackGenerator.generateFinal(promptInput.toString());

        session.complete(finalScore, finalResult.finalFeedback());
        sessionRepository.save(session);

        FinalFeedbackResponse response = new FinalFeedbackResponse(
                sessionId,
                finalScore,
                finalResult.finalFeedback(),
                questionFeedbacks
        );

        sseEmitterManager.send(sessionId, "final-complete", response);
        sseEmitterManager.complete(sessionId);

        log.info("[최종평가] 완료 - sessionId: {}, finalScore: {}", sessionId, finalScore);
    }

    private void deleteTempFile(Path videoPath) {
        if (videoPath == null) return;
        try {
            Files.deleteIfExists(videoPath);
            log.info("[임시파일] 삭제 완료 - path: {}", videoPath);
        } catch (IOException e) {
            log.warn("[임시파일] 삭제 실패 - path: {}", videoPath, e);
        }
    }

    private void sendFailureToSession(UUID answerId, String message) {
        InterviewAnswer answer = answerRepository.findByIdWithQuestionAndSession(answerId).orElse(null);
        if (answer != null && answer.getInterviewQuestion().getInterviewSession() != null) {
            UUID sessionId = answer.getInterviewQuestion().getInterviewSession().getId();
            sseEmitterManager.send(sessionId, "analysis-failed", Map.of("message", message));
            sseEmitterManager.complete(sessionId);
        }
    }
}
