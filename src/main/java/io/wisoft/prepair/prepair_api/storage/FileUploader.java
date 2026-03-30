package io.wisoft.prepair.prepair_api.storage;

import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploader {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;

    @Value("${cloud.aws.s3.presigned-url-expiration}")
    private long presignedUrlExpiration;

    public String upload(MultipartFile file, String email) {
        try {
            String extension = getExtension(file);
            String key = "interview-video/" + email + "/" + LocalDate.now() + "/" + UUID.randomUUID() + extension;

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            String url = endpoint + "/" + bucket + "/" + key;
            log.info("파일 업로드 완료 - url: {}", url);
            return url;
        } catch (IOException e) {
            log.error("파일 업로드 실패", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public Path download(String mediaUrl) {
        try {
            String key = extractKey(mediaUrl);
            Path tempFile = Files.createTempFile("video-", ".tmp");

            s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build(), tempFile);

            log.info("파일 다운로드 완료 - key: {}", key);
            return tempFile;
        } catch (Exception e) {
            log.error("파일 다운로드 실패", e);
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    public void delete(String mediaUrl) {
        String key = extractKey(mediaUrl);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
        );
        log.info("파일 삭제 완료 - key: {}", key);
    }

    public Path download(String mediaUrl) {
        try {
            String key = extractKey(mediaUrl);
            Path tempFile = Files.createTempFile("video-", ".tmp");

            s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build(), tempFile);

            log.info("파일 다운로드 완료 - key: {}", key);
            return tempFile;
        } catch (Exception e) {
            log.error("파일 다운로드 실패", e);
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    public String generatePresignedUrl(String mediaUrl) {
        String key = extractKey(mediaUrl);
        String presignedUrl = s3Presigner.presignGetObject(
                p -> p.signatureDuration(Duration.ofSeconds(presignedUrlExpiration))
                        .getObjectRequest(
                                g -> g.bucket(bucket).key(key).build()).build()
        ).url().toString();

        log.info("Presigned URL 발급 - key: {}", key);
        return presignedUrl;
    }

    private String extractKey(String mediaUrl) {
        String prefix = endpoint + "/" + bucket + "/";
        return mediaUrl.substring(prefix.length());
    }

    private String getExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            return ".webm";
        }
        return original.substring(original.lastIndexOf("."));
    }
}
