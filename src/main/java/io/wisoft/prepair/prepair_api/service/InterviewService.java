package io.wisoft.prepair.prepair_api.service;

import io.wisoft.prepair.prepair_api.entity.JobPosting;
import io.wisoft.prepair.prepair_api.entity.enums.Notification;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.client.member.dto.MemberInfo;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.global.email.EmailService;
import io.wisoft.prepair.prepair_api.prompt.InterviewPromptBuilder;
import io.wisoft.prepair.prepair_api.repository.InterviewQuestionRepository;
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
public class InterviewService {

    private final InterviewQuestionRepository questionRepository;
    private final InterviewQuestionService interviewQuestionService;
    private final MemberServiceClient memberServiceClient;
    private final OpenAiClient openAiClient;
    private final InterviewPromptBuilder promptBuilder;
    private final EmailService emailService;

    public void generateTodayQuestions() {
        List<MemberInfo> members = memberServiceClient.getMembers();
        DayOfWeek today = LocalDate.now(ZoneId.of("Asia/Seoul")).getDayOfWeek();

        List<MemberInfo> targetMembers = members.stream()
                .filter(this::isValidFrequency)
                .filter(this::isValidJob)
                .filter(this::isValidNotification)
                .filter(member -> shouldSendToday(member, today))
                .toList();

        log.info("질문 생성 대상: {} / {} 명", targetMembers.size(), members.size());
        targetMembers.forEach(this::processTodayQuestion);
    }

    private void processTodayQuestion(MemberInfo member) {
        try {
            List<InterviewQuestion> previousQuestions = questionRepository.findByMemberId(member.id());
            String prompt = promptBuilder.buildQuestionPrompt(member.job(), previousQuestions);
            QuestionWithTags result = openAiClient.generateQuestion(prompt);
            log.info("질문 생성 성공 - memberId: {}", member.id());

            InterviewQuestion question = interviewQuestionService.saveTodayQuestion(member.id(), result);
            log.info("질문 저장 성공 - memberId: {}", member.id());

            notifyMember(member, question);
        } catch (Exception e) {
            log.error("질문 생성 실패 - memberId: {}", member.id(), e);
        }
    }

    /**
     * [기업 맞춤 면접 질문 로직] - 구현 예정
     */
    public void generateCompanyQuestions(MemberInfo member, JobPosting jobPosting) {

    }

    /**
     * [기업 맞춤 면접 질문 로직] - 구현 예정
     */
    private void processCompanyQuestion(MemberInfo memberInfo, JobPosting jobPosting) {

    }

    private void notifyMember(MemberInfo member, InterviewQuestion question) {
        Notification notification = member.notification();

        if (notification == Notification.EMAIL || notification == Notification.BOTH) {
            sendEmail(member, question);
        }

        if (notification == Notification.KAKAO || notification == Notification.BOTH) {
            sendKakao(member, question);
        }
    }

    private void sendEmail(MemberInfo member, InterviewQuestion question) {
        try {
            emailService.sendInterviewQuestion(
                    member.email(),
                    member.nickname(),
                    question.getQuestionTag(),
                    question.getQuestion()
            );
            log.info("이메일 발송 성공 - memberId: {}", member.id());
        } catch (Exception e) {
            log.error("이메일 발송 실패 - memberId: {}", member.id(), e);
        }
    }

    /**
     * [카카오 알림] - 구현 예정
     */
    private void sendKakao(MemberInfo member, InterviewQuestion question) {

    }

    private boolean shouldSendToday(MemberInfo member, DayOfWeek today) {
        return switch (member.frequency()) {
            case EVERY -> true;
            case WEEKLY -> today == DayOfWeek.MONDAY;
        };
    }

    private boolean isValidNotification(MemberInfo member) {
        if (member.notification() == null) {
            log.warn("유효하지 않은 멤버 스킵 - memberId: {}, reason: no_notification", member.id());
            return false;
        }
        return true;
    }

    private boolean isValidFrequency(MemberInfo member) {
        if (member.frequency() == null) {
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
