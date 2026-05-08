package io.wisoft.prepair.prepair_api.scheduler;

import io.wisoft.prepair.prepair_api.interview.question.service.TodayQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TodayQuestionScheduler {

    private final TodayQuestionService todayQuestionService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void generateTodayQuestions() {
        String correlationId = "SCHEDULER-" + UUID.randomUUID();
        MDC.put("correlationId", correlationId);
        long startTime = System.currentTimeMillis();

        try {
            log.info("오늘의 질문 생성 스케줄러 시작");
            todayQuestionService.sendTodayQuestions();
            log.info("오늘의 질문 생성 스케줄러 종료 | elapsed={}ms", System.currentTimeMillis() - startTime);
        } catch(Exception e) {
            log.error("오늘의 질문 생성 스케줄러 실패 | elapsed={}ms", System.currentTimeMillis() - startTime, e);
        }
        finally {
            MDC.remove("correlationId");
        }
    }
}
