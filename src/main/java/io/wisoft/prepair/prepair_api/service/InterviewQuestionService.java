package io.wisoft.prepair.prepair_api.service;

import io.wisoft.prepair.prepair_api.controller.dto.response.QuestionResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.JobPosting;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.repository.InterviewQuestionRepository;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InterviewQuestionService {

    private final InterviewQuestionRepository questionRepository;

    public InterviewQuestion saveTodayQuestion(UUID memberId, QuestionWithTags result) {
        return save(memberId, QuestionType.TEXT, null, result);
    }

    public InterviewQuestion saveCompanyQuestion(UUID memberId, QuestionWithTags result, JobPosting jobPosting) {
        return save(memberId, QuestionType.COMPANY, jobPosting, result);
    }

    public InterviewQuestion saveVideoQuestion(UUID memberId, QuestionWithTags result) {
        return save(memberId, QuestionType.VIDEO, null, result);
    }

    public InterviewQuestion save(UUID memberId, QuestionType questionType, JobPosting jobPosting, QuestionWithTags result) {
        InterviewQuestion question = new InterviewQuestion(
                memberId,
                result.question(),
                questionType,
                result.joinTags(),
                jobPosting
        );
        return questionRepository.save(question);
    }
}
