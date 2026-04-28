package io.wisoft.prepair.prepair_api.interview.question.service;

import io.wisoft.prepair.prepair_api.interview.question.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.interview.jobposting.entity.JobPosting;
import io.wisoft.prepair.prepair_api.interview.question.entity.QuestionType;
import io.wisoft.prepair.prepair_api.external.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.interview.question.repository.QuestionRepository;

import java.util.UUID;

import io.wisoft.prepair.prepair_api.interview.session.repository.SessionRepository;
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
