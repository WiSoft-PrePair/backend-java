package io.wisoft.prepair.prepair_api.notification.email;

public interface EmailService {

    void sendInterviewQuestion(
            String email,
            String nickname,
            String questionTag,
            String question
    );
}
