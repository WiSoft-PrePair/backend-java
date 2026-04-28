package io.wisoft.prepair.prepair_api.interview.answer.service;

import io.wisoft.prepair.prepair_api.external.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.common.exception.BusinessException;
import io.wisoft.prepair.prepair_api.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechToTextService {

    private final OpenAiClient openAiClient;

    /**
     * 비동기 처리용 — 동기 구간에서 디스크에 저장한 Path를 받아 변환
     */
    public String convertToTextFromPath(Path inputPath, String questionTags) {
        Path outputPath = null;

        try {
            outputPath = Files.createTempFile("audio-", ".mp3");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", inputPath.toString(), outputPath.toString()
            );
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

            int exitCode = processBuilder.start().waitFor();

            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.VIDEO_CONVERSION_FAILED);
            }

            return openAiClient.speechToText(outputPath, questionTags);
        } catch (Exception e) {
            log.error("영상 변환 실패", e);
            throw new BusinessException(ErrorCode.VIDEO_CONVERSION_FAILED);
        } finally {
            deleteTempFile(outputPath);
        }
    }

    private void deleteTempFile(Path path) {
        try {
            if (path != null) Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("임시 파일 삭제 실패: {}", path);
        }
    }
}
