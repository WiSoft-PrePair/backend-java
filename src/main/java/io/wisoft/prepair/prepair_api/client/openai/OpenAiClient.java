package io.wisoft.prepair.prepair_api.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisoft.prepair.prepair_api.client.openai.dto.OpenAiRequest;
import io.wisoft.prepair.prepair_api.client.openai.dto.OpenAiResponse;
import io.wisoft.prepair.prepair_api.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${external.openai.api-key}")
    private String apiKey;

    @Value("${external.openai.api-url}")
    private String apiUrl;

    @Value("${external.openai.model}")
    private String model;

    public QuestionWithTags generateQuestion(String prompt) {
        log.info("OpenAI API 호출 - model: {}", model);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        OpenAiRequest request = OpenAiRequest.of(model, prompt);

        HttpEntity<OpenAiRequest> httpEntity = new HttpEntity<>(request, headers);

        try {
            // OpenAI API 요청
            OpenAiResponse response = restTemplate.postForObject(
                    apiUrl,
                    httpEntity,
                    OpenAiResponse.class
            );

            // Response null 체크
            if (response == null) {
                log.error("OpenAI 응답이 null입니다.");
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }

            // Choices 유효성 체크
            if (response.choices() == null || response.choices().isEmpty()) {
                log.error("OpenAI 응답에 choices가 없습니다.");
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }

            String content = response.getContent();
            QuestionWithTags result = objectMapper.readValue(content, QuestionWithTags.class);

            log.info("OpenAI 질문 생성 성공 - question: {}, tags: {}", result.question(), result.tags());

            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("OpenAI 응답 파싱 실패", e);
            throw new BusinessException(ErrorCode.OPENAI_RESPONSE_PARSE_ERROR);
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패", e);
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
    }
}
