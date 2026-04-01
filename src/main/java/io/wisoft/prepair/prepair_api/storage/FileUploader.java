package io.wisoft.prepair.prepair_api.storage;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploader {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;

    public String upload(Path videoPath, String contentType, String email) {
        String extension = getExtension(videoPath.getFileName().toString());
        String key = "interview-video/" + email + "/" + LocalDate.now() + "/" + UUID.randomUUID() + extension;

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromFile(videoPath)
        );

        return endpoint + "/" + bucket + "/" + key;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".webm";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
