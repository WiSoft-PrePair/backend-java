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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class FrameExtractor {

    private static final int SAMPLE_COUNT = 15;
    private static final long FFMPEG_TIMEOUT_SECONDS = 120;

    public List<String> extractFrames(MultipartFile video) {
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("frames-");
            String extension = getExtension(video.getOriginalFilename());
            Path tempVideo = tempDir.resolve(UUID.randomUUID() + extension);
            video.transferTo(tempVideo.toFile());

            // FFmpeg으로 1초당 1프레임 추출
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", tempVideo.toString(),
                    "-vf", "fps=1",
                    "-q:v", "2",
                    tempDir.resolve("frame_%04d.jpg").toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // stdout+stderr를 별도 스레드로 drain하여 파이프 버퍼 블로킹 방지
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

            // 추출된 프레임에서 대표 프레임 샘플링
            return sampleFrames(tempDir);
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

    private void drainStream(InputStream is) {
        Thread.ofVirtual().start(() -> {
            try (is) {
                byte[] buf = new byte[8192];
                while (is.read(buf) != -1) {
                    // 출력을 소비만 하고 버림
                }
            } catch (IOException ignored) {
            }
        });
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

        // 균등 간격으로 대표 프레임 선택
        List<String> base64Frames = new ArrayList<>();
        int interval = Math.max(1, frames.size() / SAMPLE_COUNT);

        for (int i = 0; i < frames.size() && base64Frames.size() < SAMPLE_COUNT; i += interval) {
            byte[] bytes = Files.readAllBytes(frames.get(i).toPath());
            base64Frames.add(Base64.getEncoder().encodeToString(bytes));
        }

        log.info("프레임 샘플링 완료 - 전체: {}, 선택: {}", frames.size(), base64Frames.size());
        return base64Frames;
    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".mp4";
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