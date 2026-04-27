package io.wisoft.prepair.prepair_api.dto.response;


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