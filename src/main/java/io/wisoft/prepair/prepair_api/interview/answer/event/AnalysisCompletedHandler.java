package io.wisoft.prepair.prepair_api.interview.answer.event;

import io.wisoft.prepair.prepair_api.interview.answer.service.AnalysisCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisCompletedHandler {

    private final AnalysisCompletionService analysisCompletionService;

    @Async("videoTaskExecutor")
    @EventListener
    public void handle(AnalysisCompletedEvent event) {
        UUID answerId = event.answerId();

        if (event.hasFailed()) {
            log.error("[종합평가] 분석 실패로 종합평가 생략 - answerId: {}", answerId);
            analysisCompletionService.failSession(answerId, "분석 중 오류가 발생했습니다.");
            return;
        }

        try {
            analysisCompletionService.processAnalysisCompletion(answerId);
        } catch (Exception e) {
            log.error("[종합평가] 실패 - answerId: {}, error: {}", answerId, e.getMessage(), e);
            analysisCompletionService.failSession(answerId, "종합 평가 생성 중 오류가 발생했습니다.");
        }
    }
}
