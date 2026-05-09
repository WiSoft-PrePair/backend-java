package io.wisoft.prepair.prepair_api.interview.question.service;

import io.wisoft.prepair.prepair_api.external.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.external.member.dto.MemberSchedulerInfo;
import io.wisoft.prepair.prepair_api.external.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.external.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.interview.prompt.PromptBuilder;
import io.wisoft.prepair.prepair_api.interview.question.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.interview.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodayQuestionService {

    private final QuestionPersistenceService questionPersistenceService;
    private final QuestionNotificationService questionNotificationService;
    private final QuestionRepository questionRepository;
    private final MemberServiceClient memberServiceClient;
    private final OpenAiClient openAiClient;
    private final PromptBuilder promptBuilder;

    public void sendTodayQuestions() {
        List<MemberSchedulerInfo> members = memberServiceClient.getMembers();
        DayOfWeek today = LocalDate.now(ZoneId.of("Asia/Seoul")).getDayOfWeek();

        List<MemberSchedulerInfo> targetMembers = members.stream()
                .filter(this::isValidFrequency)
                .filter(this::isValidJob)
                .filter(this::isValidNotification)
                .filter(member -> shouldSendToday(member, today))
                .toList();

        log.info("질문 생성 대상: {} / {} 명", targetMembers.size(), members.size());
        targetMembers.forEach(this::generateAndSend);
    }

    private void generateAndSend(MemberSchedulerInfo member) {
        try {
            List<InterviewQuestion> previousQuestions = questionRepository.findByMemberId(member.id());
            String prompt = promptBuilder.buildDailyQuestionPrompt(member.job(), previousQuestions);
            QuestionWithTags result = openAiClient.generateQuestion(prompt);
            log.info("질문 생성 완료 | memberId={}", member.id());

            InterviewQuestion question = questionPersistenceService.saveTodayQuestion(member.id(), result);
            log.info("질문 저장 완료 | memberId={}", member.id());

            questionNotificationService.notifyMember(member, question);
        } catch (Exception e) {
            log.error("질문 처리 실패 | memberId={}", member.id(), e);
        }
    }

    private boolean isValidFrequency(MemberSchedulerInfo member) {
        if (member.frequency() == null) {
            log.warn("멤버 스킵 | memberId={} | reason=no_frequency", member.id());
            return false;
        }
        return true;
    }

    private boolean isValidJob(MemberSchedulerInfo member) {
        if (member.job() == null || member.job().isBlank()) {
            log.warn("멤버 스킵 | memberId={} | reason=no_job", member.id());
            return false;
        }
        return true;
    }

    private boolean isValidNotification(MemberSchedulerInfo member) {
        if (member.notification() == null) {
            log.warn("멤버 스킵 | memberId={} | reason=no_notification", member.id());
            return false;
        }
        return true;
    }

    private boolean shouldSendToday(MemberSchedulerInfo member, DayOfWeek today) {
        return switch (member.frequency()) {
            case EVERY -> true;
            case WEEKLY -> today == DayOfWeek.MONDAY;
        };
    }
}
