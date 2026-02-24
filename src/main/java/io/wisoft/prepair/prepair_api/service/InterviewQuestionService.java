package io.wisoft.prepair.prepair_api.service;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
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

    public InterviewQuestion saveCompanyQuestion(UUID memberId, QuestionWithTags result, String sourceRef) {
        return save(memberId, QuestionType.COMPANY, sourceRef, result);
    }

    public InterviewQuestion save(UUID memberId, QuestionType questionType, String sourceRef, QuestionWithTags result) {
        InterviewQuestion question = new InterviewQuestion(
                memberId,
                result.question(),
                questionType,
                result.joinTags(),
                sourceRef
        );
        return questionRepository.save(question);
    }
}
