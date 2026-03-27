package io.wisoft.prepair.prepair_api.service;

import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechToTextService {

    private final OpenAiClient openAiClient;

    public String transcribe(MultipartFile video, String tags) {
        Path inputPath = null;
        Path outputPath = null;

        try {
            inputPath = Files.createTempFile("video-", ".tmp");
            outputPath = Files.createTempFile("audio-", ".mp3");
            video.transferTo(inputPath);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", inputPath.toString(), outputPath.toString()
            );
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);

            int exitCode = pb.start().waitFor();

            if (exitCode != 0) {
                throw new BusinessException(ErrorCode.VIDEO_CONVERSION_FAILED);
            }

            return openAiClient.transcribe(outputPath, tags);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VIDEO_CONVERSION_FAILED);
        } finally {
            deleteTempFile(inputPath);
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
