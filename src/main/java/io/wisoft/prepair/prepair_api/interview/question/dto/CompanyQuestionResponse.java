package io.wisoft.prepair.prepair_api.interview.question.dto;

import io.wisoft.prepair.prepair_api.interview.jobposting.dto.JobPostingResponse;

import java.util.List;

public record CompanyQuestionResponse(
        JobPostingResponse jobPosting,
        List<QuestionResponse> questions
) {
    public static CompanyQuestionResponse of(JobPostingResponse jobPosting, List<QuestionResponse> questions) {
        return new CompanyQuestionResponse(
                jobPosting,
                questions
        );
    }
}
