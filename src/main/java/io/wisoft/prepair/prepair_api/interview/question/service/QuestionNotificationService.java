package io.wisoft.prepair.prepair_api.interview.question.service;

import io.wisoft.prepair.prepair_api.external.member.dto.MemberSchedulerInfo;
import io.wisoft.prepair.prepair_api.external.member.enums.Notification;
import io.wisoft.prepair.prepair_api.external.notification.email.EmailService;
import io.wisoft.prepair.prepair_api.external.notification.kakao.KakaoService;
import io.wisoft.prepair.prepair_api.interview.question.entity.InterviewQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionNotificationService {

    private final EmailService emailService;
    private final KakaoService kakaoService;

    public void notifyMember(MemberSchedulerInfo member, InterviewQuestion question) {
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
            emailService.send(
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
        if (member.kakaoAccessToken() == null || member.kakaoAccessToken().isBlank()) {
            log.warn("멤버 스킵 | memberId={} | reason=no_kakao_token", member.id());
            return;
        }

        try {
            kakaoService.send(
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
}
