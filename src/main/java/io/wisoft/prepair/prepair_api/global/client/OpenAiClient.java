package io.wisoft.prepair.prepair_api.global.client;

import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final String model;

    public OpenAiClient(
            @Value("${external.openai.api-key}") final String apiKey,
            @Value("${external.openai.api-url}") final String apiUrl,
            @Value("${external.openai.model}") final String model
    ) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String chat(final String systemPrompt, final String userMessage) {
        final var messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        );

        return callApi(messages);
    }

    private String callApi(final List<?> messages) {
        final var requestBody = Map.of(
                "model", model,
                "messages", messages
        );

        try {
            final var response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            final var choices = (List<Map<String, Object>>) response.get("choices");
            final var message = (Map<String, Object>) choices.getFirst().get("message");
            return (String) message.get("content");
        } catch (final Exception e) {
            log.error("OpenAI API 호출 실패 - " + e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new BusinessException(ErrorCode.LLM_API_FAILED);
        }
    }
}
