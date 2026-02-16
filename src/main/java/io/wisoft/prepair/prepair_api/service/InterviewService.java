package io.wisoft.prepair.prepair_api.service;

import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.client.member.dto.MemberInfo;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import io.wisoft.prepair.prepair_api.prompt.InterviewPromptBuilder;
import io.wisoft.prepair.prepair_api.repository.InterviewQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewQuestionRepository questionRepository;
    private final MemberServiceClient memberServiceClient;
    private final OpenAiClient openAiClient;
    private final InterviewPromptBuilder promptBuilder;

    @Transactional
    public void generateTodayQuestions() {
        List<MemberInfo> members = memberServiceClient.getMembers();
        DayOfWeek today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).getDayOfWeek();

        List<MemberInfo> targetMembers = members.stream()
                .filter(this::isValidFrequency)
                .filter(this::isValidJob)
                .filter(member -> shouldSendToday(member, today))
                .toList();

        log.info("질문 생성 대상: {} / {} 명", targetMembers.size(), members.size());

        for (MemberInfo member : targetMembers) {
            try {
                List<InterviewQuestion> previousQuestions = questionRepository.findByMemberId(member.id());
                String prompt = promptBuilder.buildQuestionPrompt(member.job(), previousQuestions);
                generateAndSaveQuestion(member.id(), prompt, QuestionType.TEXT, null);
                log.info("질문 생성 성공 - memberId: {}", member.id());
            } catch (Exception e) {
                log.error("질문 생성 실패 - memberId: {}", member.id(), e);
            }
        }
    }

    private InterviewQuestion generateAndSaveQuestion(UUID memberId, String prompt, QuestionType questionType, String sourceRef) {
        QuestionWithTags result = openAiClient.generateQuestion(prompt);

        InterviewQuestion question = new InterviewQuestion(
                memberId,
                result.question(),
                questionType,
                result.joinTags(),
                sourceRef
        );
        return questionRepository.save(question);
    }

    private boolean shouldSendToday(MemberInfo member, DayOfWeek today) {
        return switch (member.frequency()) {
            case "daily" -> true;
            case "weekly" -> today == DayOfWeek.MONDAY;
            default -> false;
        };
    }

    private boolean isValidFrequency(MemberInfo member) {
        if (member.frequency() == null || member.frequency().isBlank()) {
            log.warn("유효하지 않은 멤버 스킵 - memberId: {}, reason: no_frequency", member.id());
            return false;
        }
        return true;
    }

    private boolean isValidJob(MemberInfo member) {
        if (member.job() == null || member.job().isBlank()) {
            log.warn("유효하지 않은 멤버 스킵 - memberId: {}, reason: no_job", member.id());
            return false;
        }
        return true;
    }
}
