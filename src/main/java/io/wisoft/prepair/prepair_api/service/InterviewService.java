package io.wisoft.prepair.prepair_api.service;

import io.wisoft.prepair.prepair_api.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.client.user.UserServiceClient;
import io.wisoft.prepair.prepair_api.dto.response.TodayQuestionResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import io.wisoft.prepair.prepair_api.prompt.InterviewPromptBuilder;
import io.wisoft.prepair.prepair_api.repository.InterviewQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewQuestionRepository questionRepository;
    private final UserServiceClient userServiceClient;
    private final OpenAiClient openAiClient;
    private final InterviewPromptBuilder promptBuilder;

    @Transactional
    public TodayQuestionResponse createTodayQuestion(String userId) {
        UUID userUuid = UUID.fromString(userId);

        // 이전 질문 조회 (중복 방지)
        List<InterviewQuestion> previousQuestions = questionRepository
                .findByUserId(userUuid);

        // 직무 정보 조회
        String job = userServiceClient.getJob(userUuid);

        // 프롬프트 생성
        String prompt = promptBuilder.buildQuestionPrompt(job, previousQuestions);

        // OpenAI API 호출
        QuestionWithTags result = openAiClient.generateQuestion(prompt);

        // Entity 생성 및 저장
        InterviewQuestion question = new InterviewQuestion(
                userUuid,
                result.question(),
                QuestionType.TEXT,
                result.joinTags(),
                null
        );

        InterviewQuestion saved = questionRepository.save(question);
        log.info("오늘의 질문 저장 완료 - userId: {}, questionId: {}", userUuid, saved.getId());

        return TodayQuestionResponse.from(saved);
    }
}
