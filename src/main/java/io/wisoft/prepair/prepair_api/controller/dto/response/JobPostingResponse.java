package io.wisoft.prepair.prepair_api.controller.dto.response;

import io.wisoft.prepair.prepair_api.entity.JobPosting;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * test response
 */
public record JobPostingResponse(
        UUID id,
        String sourceUrl,
        String sourceSite,
        String content,
        String rawContent,
        LocalDateTime createdAt
) {
    public static JobPostingResponse from(final JobPosting jobPosting) {
        return new JobPostingResponse(
                jobPosting.getId(),
                jobPosting.getSourceUrl(),
                jobPosting.getSourceType().name(),
                jobPosting.getContent(),
                jobPosting.getRawContent(),
                jobPosting.getCreatedAt()
        );
    }
}