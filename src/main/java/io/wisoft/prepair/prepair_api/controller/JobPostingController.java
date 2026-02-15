package io.wisoft.prepair.prepair_api.controller;

import io.wisoft.prepair.prepair_api.controller.dto.JobPostingRequest;
import io.wisoft.prepair.prepair_api.controller.dto.JobPostingResponse;
import io.wisoft.prepair.prepair_api.entity.JobPosting;
import io.wisoft.prepair.prepair_api.global.common.ApiResponse;
import io.wisoft.prepair.prepair_api.service.JobPostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;

    @PostMapping("/me/company")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<JobPostingResponse> crawl(@Valid @RequestBody final JobPostingRequest request) {
        final JobPosting jobPosting = jobPostingService.crawlAndSave(request.url());
        return new ApiResponse<>(200, JobPostingResponse.from(jobPosting), "채용공고 크롤링 완료");
    }
}