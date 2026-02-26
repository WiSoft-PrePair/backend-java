package io.wisoft.prepair.prepair_api.controller.dto.response;

import io.wisoft.prepair.prepair_api.entity.JobPosting;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;

import java.util.List;

public record CompanyQuestionResponse(
        JobPostingResponse jobPosting,
        List<QuestionResponse> questions
) {

    public static CompanyQuestionResponse of(JobPosting jobPosting, List<InterviewQuestion> questions) {
        return new CompanyQuestionResponse(
                JobPostingResponse.from(jobPosting),
                questions.stream().map(QuestionResponse::from).toList()
        );
    }
}