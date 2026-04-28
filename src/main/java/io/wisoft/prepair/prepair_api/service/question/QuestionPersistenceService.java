package io.wisoft.prepair.prepair_api.service.question;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.entity.JobPosting;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.repository.QuestionRepository;

import java.util.UUID;

import io.wisoft.prepair.prepair_api.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionPersistenceService {

    private final QuestionRepository questionRepository;
    private final SessionRepository sessionRepository;

    public InterviewQuestion saveTodayQuestion(UUID memberId, QuestionWithTags result) {
        return save(memberId,
                QuestionType.TEXT,
                null,
                result,
                null);
    }

    public InterviewQuestion saveCompanyQuestion(UUID memberId, QuestionWithTags result, JobPosting jobPosting) {
        return save(memberId,
                QuestionType.COMPANY,
                jobPosting,
                result,
                null);
    }

    public InterviewQuestion saveVideoQuestion(UUID memberId, QuestionWithTags result, InterviewSession session) {
        return save(memberId,
                QuestionType.VIDEO,
                null,
                result,
                session);
    }

    private InterviewQuestion save(UUID memberId, QuestionType questionType, JobPosting jobPosting, QuestionWithTags result, InterviewSession session) {
        InterviewQuestion question = new InterviewQuestion(
                memberId,
                result.question(),
                questionType,
                result.joinTags(),
                jobPosting,
                session
        );
        return questionRepository.save(question);
    }

    public InterviewSession createSession(UUID memberId, int totalQuestionCount) {
        return sessionRepository.save(new InterviewSession(memberId, totalQuestionCount));
    }
}
