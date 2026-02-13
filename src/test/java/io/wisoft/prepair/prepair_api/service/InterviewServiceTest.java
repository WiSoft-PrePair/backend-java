package io.wisoft.prepair.prepair_api.service;

import io.wisoft.prepair.prepair_api.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.client.user.UserServiceClient;
import io.wisoft.prepair.prepair_api.dto.response.TodayQuestionResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionStatus;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import io.wisoft.prepair.prepair_api.prompt.InterviewPromptBuilder;
import io.wisoft.prepair.prepair_api.repository.InterviewQuestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewQuestionRepository questionRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private OpenAiClient openAiClient;

    @Mock
    private InterviewPromptBuilder promptBuilder;

    @InjectMocks
    private InterviewService interviewService;

    @Test
    @DisplayName("오늘의 질문이 생성되고 저장된다.")
    void 오늘의_질문_생성_및_저장() {
        // Given
        UUID userId = UUID.randomUUID();
        String job = "백엔드 개발자";
        String prompt = "당신은 백엔드 개발자 면접관입니다.";
        List<InterviewQuestion> previousQuestions = List.of();

        QuestionWithTags aiResponse = new QuestionWithTags(
                "Spring Boot의 IoC 컨테이너에 대해 설명해주세요.",
                List.of("Spring", "IoC", "DI")
        );

        InterviewQuestion savedQuestion = new InterviewQuestion(
                userId,
                aiResponse.question(),
                QuestionType.TEXT,
                aiResponse.joinTags(),
                null
        );

        given(questionRepository.findByUserId(userId)).willReturn(previousQuestions);
        given(userServiceClient.getJob(userId)).willReturn(job);
        given(promptBuilder.buildQuestionPrompt(any(), any())).willReturn(prompt);
        given(openAiClient.generateQuestion(any())).willReturn(aiResponse);
        given(questionRepository.save(any(InterviewQuestion.class))).willReturn(savedQuestion);

        // When
        TodayQuestionResponse result = interviewService.createTodayQuestion(userId.toString());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.question()).isEqualTo("Spring Boot의 IoC 컨테이너에 대해 설명해주세요.");
        assertThat(result.questionType()).isEqualTo(QuestionType.TEXT);
        assertThat(result.questionTag()).isEqualTo("Spring,IoC,DI");
        assertThat(result.status()).isEqualTo(QuestionStatus.UNANSWERED);
        assertThat(result.latestScore()).isNull();
        assertThat(result.sourceRef()).isNull();
        // createdAt은 JPA Auditing으로 설정되므로 Mock 테스트에서는 null

        verify(questionRepository).findByUserId(userId);
        verify(userServiceClient).getJob(userId);
        verify(promptBuilder).buildQuestionPrompt(job, previousQuestions);
        verify(openAiClient).generateQuestion(prompt);
        verify(questionRepository).save(any(InterviewQuestion.class));
    }
}