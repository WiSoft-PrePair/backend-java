package io.wisoft.prepair.prepair_api.scheduler;

import io.wisoft.prepair.prepair_api.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodayQuestionScheduler {

    private final InterviewService interviewService;

    @Scheduled(cron = "0 */5 * * * *")
    public void generateTodayQuestions() {
        String correlationId = "SCHEDULER-" + UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            log.info("오늘의 질문 생성 스케줄러 시작 - {}", LocalDateTime.now());
            interviewService.generateTodayQuestions();
            log.info("오늘의 질문 생성 스케줄러 종료");
        } finally {
            MDC.remove("correlationId");
        }
    }
}
