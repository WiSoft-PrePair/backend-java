package io.wisoft.prepair.prepair_api.service.answer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.prompt.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackGenerator {

    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final OpenAiClient openAiClient;

    /**
     * 질문과 답변을 기반으로 OpenAI 피드백을 생성한다.
     * 프롬프트 생성 → OpenAI 호출 → JSON 파싱을 일괄 처리한다.
     */
    public FeedbackResult generate(final InterviewQuestion question, final String answer) {
        String prompt = promptBuilder.buildFeedbackPrompt(question.getQuestion(), question.getQuestionTag(), answer);
        String raw = openAiClient.generateText(prompt);

        try {
            return objectMapper.readValue(raw, FeedbackResult.class);
        } catch (JsonProcessingException e) {
            log.error("피드백 응답 파싱 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENAI_RESPONSE_PARSE_ERROR);
        }
    }
}
