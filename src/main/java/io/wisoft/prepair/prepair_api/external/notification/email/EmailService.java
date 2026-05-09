package io.wisoft.prepair.prepair_api.external.notification.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailTemplateBuilder emailTemplateBuilder;
    private final JavaMailSender mailSender;

    @Retryable(
            value = {MessagingException.class, MailException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void send(String email, String nickname, String questionTag, String question) throws MessagingException {
        String html = emailTemplateBuilder.buildInterviewQuestionHtml(nickname, questionTag, question);
        sendMail(email, "[PrePair] 오늘의 면접 질문이 도착했어요!", html);
    }

    private void sendMail(String to, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);

        mailSender.send(message);
    }
}