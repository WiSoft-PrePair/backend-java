package io.wisoft.prepair.prepair_api.controller;

import io.wisoft.prepair.prepair_api.controller.dto.request.JobPostingRequest;
import io.wisoft.prepair.prepair_api.controller.dto.response.CompanyQuestionResponse;
import io.wisoft.prepair.prepair_api.entity.JobPosting;
import io.wisoft.prepair.prepair_api.global.common.ApiResponse;
import io.wisoft.prepair.prepair_api.service.InterviewService;
import io.wisoft.prepair.prepair_api.service.JobPostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final InterviewService interviewService;

    @PostMapping("/me/company")
    public ApiResponse<CompanyQuestionResponse> crawl(
            @RequestHeader("X-User-Id") final UUID memberId,
            @Valid @RequestBody final JobPostingRequest request
    ) {
        final JobPosting jobPosting = jobPostingService.crawlAndSave(request.url());
        final CompanyQuestionResponse response = CompanyQuestionResponse.of(
                jobPosting,
                interviewService.generateCompanyQuestions(memberId, jobPosting)
        );
        return ApiResponse.ok(response, "기업 맞춤 질문 생성 완료");
    }
}