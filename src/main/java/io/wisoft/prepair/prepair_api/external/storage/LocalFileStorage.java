package io.wisoft.prepair.prepair_api.external.storage;

import io.wisoft.prepair.prepair_api.common.exception.BusinessException;
import io.wisoft.prepair.prepair_api.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class LocalFileStorage {

    public Path save(MultipartFile file) {
        try {
            Path path = Files.createTempFile("video-", FileExtensions.extract(file.getOriginalFilename()));
            file.transferTo(path);
            return path;
        } catch (Exception e) {
            log.error("[로컬파일] 저장 실패 - filename: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public void delete(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("[로컬파일] 삭제 실패 - path: {}", path, e);
        }
    }
}
