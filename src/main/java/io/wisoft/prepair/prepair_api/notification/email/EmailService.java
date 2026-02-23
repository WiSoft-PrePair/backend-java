package io.wisoft.prepair.prepair_api.notification.email;

import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailTemplateBuilder emailTemplateBuilder;
    private final JavaMailSender mailSender;

    public void sendInterviewQuestion(String email, String nickname, String questionTag, String question) {
        String html = emailTemplateBuilder.buildInterviewQuestionHtml(nickname, questionTag, question);
        send(email, "[PrePair] 오늘의 면접 질문이 도착했어요!", html);
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
