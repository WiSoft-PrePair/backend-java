package io.wisoft.prepair.prepair_api.service.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.prompt.VideoAnalysisPromptBuilder;
import io.wisoft.prepair.prepair_api.video.FrameExtractor;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAnalysisService {

    private final FrameExtractor frameExtractor;
    private final OpenAiClient openAiClient;
    private final VideoAnalysisPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public FeedbackResult analyze(final Path videoPath) {
        List<String> frames = frameExtractor.extractFrames(videoPath);
        return analyzeFrames(frames);
    }

    private FeedbackResult analyzeFrames(List<String> frames) {
        log.info("프레임 추출 완료 - {}개", frames.size());

        String prompt = promptBuilder.buildVisionPrompt();
        String result = openAiClient.analyzeWithVision(prompt, frames);
        log.info("비디오 분석 완료");

        return parseVisionResponse(result);
    }

    private FeedbackResult parseVisionResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            int overallScore = root.path("overall_score").asInt();
            String summary = root.path("summary").asText();

            String good = buildGoodFeedback(root);
            String improvement = buildImprovementFeedback(root);

            return new FeedbackResult(good, improvement, summary, overallScore);
        } catch (Exception e) {
            log.error("Vision 응답 파싱 실패", e);
            throw new BusinessException(ErrorCode.OPENAI_RESPONSE_PARSE_ERROR);
        }
    }

    private String buildGoodFeedback(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        appendHighScoreItem(sb, root, "eye_contact", "시선 처리");
        appendHighScoreItem(sb, root, "facial_expression", "표정");
        appendHighScoreItem(sb, root, "posture", "자세");
        appendHighScoreItem(sb, root, "gesture", "제스처");
        return sb.toString().trim();
    }

    private String buildImprovementFeedback(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        appendLowScoreItem(sb, root, "eye_contact", "시선 처리");
        appendLowScoreItem(sb, root, "facial_expression", "표정");
        appendLowScoreItem(sb, root, "posture", "자세");
        appendLowScoreItem(sb, root, "gesture", "제스처");
        return sb.toString().trim();
    }

    private void appendHighScoreItem(StringBuilder sb, JsonNode root, String key, String label) {
        JsonNode node = root.path(key);
        if (node.path("score").asInt() >= 70) {
            sb.append("[").append(label).append("] ").append(node.path("feedback").asText()).append("\n");
        }
    }

    private void appendLowScoreItem(StringBuilder sb, JsonNode root, String key, String label) {
        JsonNode node = root.path(key);
        if (node.path("score").asInt() < 70) {
            sb.append("[").append(label).append("] ").append(node.path("feedback").asText()).append("\n");
        }
    }
}
