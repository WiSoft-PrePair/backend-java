package io.wisoft.prepair.prepair_api.controller;

import io.wisoft.prepair.prepair_api.controller.dto.request.JobPostingRequest;
import io.wisoft.prepair.prepair_api.controller.dto.response.QuestionResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final InterviewService interviewService;

    @PostMapping("/me/company")
    public ApiResponse<List<QuestionResponse>> crawl(
            @RequestHeader("X-User-Id") final UUID memberId,
            @Valid @RequestBody final JobPostingRequest request
    ) {
        final JobPosting jobPosting = jobPostingService.crawlAndSave(request.url());
        final List<QuestionResponse> response = interviewService.generateCompanyQuestions(memberId, jobPosting)
                .stream()
                .map(QuestionResponse::from)
                .toList();
        return ApiResponse.ok(response, "기업 맞춤 질문 생성 완료");
    }
}