package io.wisoft.prepair.prepair_api.interview.jobposting.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.wisoft.prepair.prepair_api.interview.jobposting.entity.JobPosting;
import io.wisoft.prepair.prepair_api.interview.jobposting.entity.SourceType;

import java.util.UUID;

public record JobPostingResponse(
        UUID id,
        String sourceUrl,
        SourceType sourceType,
        @JsonRawValue String content
) {

    public static JobPostingResponse from(JobPosting jobPosting) {
        return new JobPostingResponse(
                jobPosting.getId(),
                jobPosting.getSourceUrl(),
                jobPosting.getSourceType(),
                jobPosting.getContent()
        );
    }
}