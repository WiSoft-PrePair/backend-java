package io.wisoft.prepair.prepair_api.scheduler;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.Notification;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.client.member.dto.MemberSchedulerInfo;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.notification.email.EmailService;
import io.wisoft.prepair.prepair_api.notification.kakao.KakaoService;
import io.wisoft.prepair.prepair_api.prompt.InterviewPromptBuilder;
import io.wisoft.prepair.prepair_api.repository.InterviewQuestionRepository;
import io.wisoft.prepair.prepair_api.service.InterviewQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodayQuestionService {

    private final InterviewQuestionRepository questionRepository;
    private final InterviewQuestionService interviewQuestionService;
    private final MemberServiceClient memberServiceClient;
    private final OpenAiClient openAiClient;
    private final InterviewPromptBuilder promptBuilder;
    private final EmailService emailService;
    private final KakaoService kakaoService;

    public void generateTodayQuestions() {
        List<MemberSchedulerInfo> members = memberServiceClient.getMembers();
        DayOfWeek today = LocalDate.now(ZoneId.of("Asia/Seoul")).getDayOfWeek();

        List<MemberSchedulerInfo> targetMembers = members.stream()
                .filter(this::isValidFrequency)
                .filter(this::isValidJob)
                .filter(this::isValidNotification)
                .filter(member -> shouldSendToday(member, today))
                .toList();

        log.info("질문 생성 대상: {} / {} 명", targetMembers.size(), members.size());
        targetMembers.forEach(this::processTodayQuestion);
    }

    private void processTodayQuestion(MemberSchedulerInfo member) {
        try {
            List<InterviewQuestion> previousQuestions = questionRepository.findByMemberId(member.id());
            String prompt = promptBuilder.buildDailyQuestionPrompt(member.job(), previousQuestions);
            QuestionWithTags result = openAiClient.generateQuestion(prompt);
            log.info("질문 생성 완료 | memberId={}", member.id());

            InterviewQuestion question = interviewQuestionService.saveTodayQuestion(member.id(), result);
            log.info("질문 저장 완료 | memberId={}", member.id());

            notifyMember(member, question);
        } catch (Exception e) {
            log.error("질문 처리 실패 | memberId={}", member.id(), e);
        }
    }

    private void notifyMember(MemberSchedulerInfo member, InterviewQuestion question) {
        Notification notification = member.notification();

        if (notification == Notification.EMAIL || notification == Notification.BOTH) {
            sendEmail(member, question);
        }

        if (notification == Notification.KAKAO || notification == Notification.BOTH) {
            sendKakao(member, question);
        }
    }

    private void sendEmail(MemberSchedulerInfo member, InterviewQuestion question) {
        try {
            emailService.sendInterviewQuestion(
                    member.email(),
                    member.nickname(),
                    question.getQuestionTag(),
                    question.getQuestion()
            );
            log.info("이메일 발송 완료 | memberId={}", member.id());
        } catch (Exception e) {
            log.error("이메일 발송 실패 | memberId={}", member.id(), e);
        }
    }

    private void sendKakao(MemberSchedulerInfo member, InterviewQuestion question) {
        if (!isValidKakaoToken(member)) return;
        try {
            kakaoService.sendInterviewQuestion(
                    member.kakaoAccessToken(),
                    question.getQuestion(),
                    question.getQuestionTag()
            );
            log.info("카카오 발송 완료 | memberId={}", member.id());
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("카카오 발송 실패 | memberId={} | reason=token_expired", member.id());
        } catch (Exception e) {
            log.error("카카오 발송 실패 | memberId={}", member.id(), e);
        }
    }

    private boolean shouldSendToday(MemberSchedulerInfo member, DayOfWeek today) {
        return switch (member.frequency()) {
            case EVERY -> true;
            case WEEKLY -> today == DayOfWeek.MONDAY;
        };
    }

    private boolean isValidKakaoToken(MemberSchedulerInfo member) {
        if (member.kakaoAccessToken() == null || member.kakaoAccessToken().isBlank()) {
            log.warn("멤버 스킵 | memberId={} | reason=no_kakao_token", member.id());
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

}
