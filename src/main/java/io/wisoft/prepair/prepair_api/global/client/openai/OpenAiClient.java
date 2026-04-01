package io.wisoft.prepair.prepair_api.global.client.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.OpenAiRequest;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.OpenAiResponse;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.video.dto.VideoRequest;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    @Value("${external.openai.whisper-url}")
    private String whisperUrl;

    @Value("${external.openai.whisper-model}")
    private String whisperModel;

    /**
     * 단일 질문 생성
     */
    public QuestionWithTags generateQuestion(String prompt) {

        final String content = call(OpenAiRequest.of(model, prompt));

        try {
            return objectMapper.readValue(content, QuestionWithTags.class);
        } catch (JsonProcessingException e) {
            log.error("OpenAI 응답 파싱 실패", e);
            throw new BusinessException(ErrorCode.OPENAI_RESPONSE_PARSE_ERROR);
        }
    }

    /**
     * 다중 질문 생성
     */
    public List<QuestionWithTags> generateQuestions(String prompt) {

        final String content = call(OpenAiRequest.of(model, prompt));

        try {
            return objectMapper.readValue(
                    content,
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException e) {
            log.error("OpenAI 응답 파싱 실패");
            throw new BusinessException(ErrorCode.OPENAI_RESPONSE_PARSE_ERROR);
        }
    }

    /**
     * JSON 구조화 요청
     */
    public String generateText(String prompt) {
        return call(OpenAiRequest.of(model, prompt));
    }

    private String call(OpenAiRequest request) {
        try {
            final OpenAiResponse response = restClient.post()
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
            return response.getContent();
        } catch (Exception e) {
            log.error("OpenAI 호출 실패", e);
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
    }

    public String analyzeWithVision(String prompt, List<String> base64Images) {
        try {
            VideoRequest request = VideoRequest.of(model, prompt, base64Images);

            OpenAiResponse response = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OpenAiResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.error("Vision 응답 없음");
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }
            return response.getContent();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Vision API 호출 실패", e);
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
    }

    public String speechToText(Path mediaPath, String questionTags) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new InputStreamResource(Files.newInputStream(mediaPath)) {
                @Override
                public String getFilename() {
                    return mediaPath.getFileName().toString();
                }

                @Override
                public long contentLength() throws IOException {
                    return Files.size(mediaPath);
                }
            });
            body.add("model", whisperModel);
            body.add("language", "ko");
            body.add("prompt", questionTags);
            body.add("temperature", "0");

            WhisperResponse response = restClient.post()
                    .uri(whisperUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(WhisperResponse.class);

            if (response == null || response.text() == null) {
                throw new BusinessException(ErrorCode.OPENAI_INVALID_RESPONSE);
            }

            return response.text();
        } catch (Exception e) {
            log.error("Whisper STT 호출 실패", e);
            throw new BusinessException(ErrorCode.STT_FAILED);
        }
    }
}
