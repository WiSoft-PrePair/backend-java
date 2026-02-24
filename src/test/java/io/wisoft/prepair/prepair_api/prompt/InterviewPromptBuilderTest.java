package io.wisoft.prepair.prepair_api.prompt;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("면접 프롬프트 생성 테스트")
class InterviewPromptBuilderTest {

    private InterviewPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new InterviewPromptBuilder();
    }

    @Test
    @DisplayName("직무 정보를 기반으로 프롬프트를 생성한다.")
    void 직무_정보_프롬프트_생성() {
        // Given
        String job = "백엔드 개발자";
        List<InterviewQuestion> previousQuestions = List.of();

        // When
        String prompt = promptBuilder.buildDailyQuestionPrompt(job, previousQuestions);

        // Then
        assertThat(prompt).contains("백엔드 개발자");
    }

    @Test
    @DisplayName("이전 질문이 있으면 프롬프트에 포함된다.")
    void 이전_질문_포함() {
        // Given
        String job = "백엔드 개발자";

        InterviewQuestion prev = new InterviewQuestion(
                UUID.randomUUID(),
                "Spring이란?",
                QuestionType.TEXT,
                "Spring",
                null
        );

        List<InterviewQuestion> previousQuestions = List.of(prev);

        // When
        String prompt = promptBuilder.buildDailyQuestionPrompt(job, previousQuestions);

        // Then
        assertThat(prompt).contains("이전에 받은 질문들");
        assertThat(prompt).contains("Spring이란?");
    }
}
