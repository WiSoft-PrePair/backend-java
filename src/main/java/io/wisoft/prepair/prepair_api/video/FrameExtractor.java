package io.wisoft.prepair.prepair_api.video;

import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FrameExtractor {

    private static final int SAMPLE_COUNT = 15;
    private static final long FFMPEG_TIMEOUT_SECONDS = 120;

    public List<String> extractFrames(Path videoPath) {
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("frames-");
            return extractFromPath(tempDir, videoPath);
        } catch (IOException e) {
            log.error("프레임 추출 실패", e);
            throw new BusinessException(ErrorCode.FRAME_EXTRACTION_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("프레임 추출 중 인터럽트 발생", e);
            throw new BusinessException(ErrorCode.FRAME_EXTRACTION_FAILED);
        } finally {
            cleanup(tempDir);
        }
    }

    private List<String> extractFromPath(Path tempDir, Path videoPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", videoPath.toString(),
                "-vf", "fps=1",
                "-q:v", "2",
                tempDir.resolve("frame_%04d.jpg").toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        drainStream(process.getInputStream());

        boolean finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.error("FFmpeg 프레임 추출 타임아웃 - {}초 초과", FFMPEG_TIMEOUT_SECONDS);
            throw new BusinessException(ErrorCode.FRAME_EXTRACTION_FAILED);
        }

        if (process.exitValue() != 0) {
            log.error("FFmpeg 프레임 추출 실패 - exitCode: {}", process.exitValue());
            throw new BusinessException(ErrorCode.FRAME_EXTRACTION_FAILED);
        }

        return sampleFrames(tempDir);
    }

    private List<String> sampleFrames(Path frameDir) throws IOException {
        List<File> frames;
        try (Stream<Path> paths = Files.list(frameDir)) {
            frames = paths
                    .filter(p -> p.toString().endsWith(".jpg"))
                    .sorted()
                    .map(Path::toFile)
                    .toList();
        }

        if (frames.isEmpty()) {
            throw new BusinessException(ErrorCode.FRAME_EXTRACTION_FAILED);
        }

        List<String> base64Frames = new ArrayList<>();
        int interval = Math.max(1, frames.size() / SAMPLE_COUNT);

        for (int i = 0; i < frames.size() && base64Frames.size() < SAMPLE_COUNT; i += interval) {
            byte[] bytes = Files.readAllBytes(frames.get(i).toPath());
            base64Frames.add(Base64.getEncoder().encodeToString(bytes));
        }

        log.info("프레임 샘플링 완료 - 전체: {}, 선택: {}", frames.size(), base64Frames.size());
        return base64Frames;
    }

    private void drainStream(InputStream is) {
        Thread.ofVirtual().start(() -> {
            try (is) {
                byte[] buf = new byte[8192];
                while (is.read(buf) != -1) {
                }
            } catch (IOException ignored) {
            }
        });
    }

    private void cleanup(Path dir) {
        if (dir == null) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }
}