package io.wisoft.prepair.prepair_api.global.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.OpenAiRequest;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.OpenAiResponse;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${external.openai.api-key}")
    private String apiKey;

    @Value("${external.openai.api-url}")
    private String apiUrl;

    @Value("${external.openai.model}")
    private String model;

    public QuestionWithTags generateQuestion(String prompt) {
        try {
            OpenAiRequest request = OpenAiRequest.of(model, prompt);

            OpenAiResponse response = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OpenAiResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.error("OpenAI 응답 없음");
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }

            String content = response.getContent();
            return objectMapper.readValue(content, QuestionWithTags.class);

        } catch (BusinessException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("OpenAI 응답 파싱 실패", e);
            throw new BusinessException(ErrorCode.OPENAI_RESPONSE_PARSE_ERROR);
        } catch (Exception e) {
            log.error("OpenAI 호출 실패", e);
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
    }
}
