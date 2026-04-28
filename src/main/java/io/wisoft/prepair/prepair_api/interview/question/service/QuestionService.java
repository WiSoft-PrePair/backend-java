package io.wisoft.prepair.prepair_api.interview.question.service;

import io.wisoft.prepair.prepair_api.interview.jobposting.dto.JobPostingRequest;
import io.wisoft.prepair.prepair_api.interview.question.dto.VideoInterviewRequest;
import io.wisoft.prepair.prepair_api.interview.question.dto.CompanyQuestionResponse;
import io.wisoft.prepair.prepair_api.interview.jobposting.dto.JobPostingResponse;
import io.wisoft.prepair.prepair_api.interview.question.dto.QuestionResponse;
import io.wisoft.prepair.prepair_api.interview.question.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.interview.session.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.interview.jobposting.entity.JobPosting;
import io.wisoft.prepair.prepair_api.interview.question.entity.QuestionType;
import io.wisoft.prepair.prepair_api.external.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.common.exception.BusinessException;
import io.wisoft.prepair.prepair_api.common.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.external.member.dto.MemberSchedulerInfo;
import io.wisoft.prepair.prepair_api.external.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.external.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.interview.prompt.PromptBuilder;
import io.wisoft.prepair.prepair_api.interview.question.repository.QuestionRepository;

import io.wisoft.prepair.prepair_api.interview.jobposting.service.JobPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionPersistenceService questionPersistService;
    private final JobPostingService jobPostingService;
    private final QuestionRepository questionRepository;
    private final MemberServiceClient memberServiceClient;
    private final OpenAiClient openAiClient;
    private final PromptBuilder promptBuilder;

    public List<QuestionResponse> getQuestions(UUID memberId, QuestionType type) {
        return questionRepository.findByMemberIdAndQuestionTypeOrderByCreatedAtDesc(memberId, type)
                .stream()
                .map(QuestionResponse::from)
                .toList();
    }

    public QuestionResponse getQuestion(UUID questionId, UUID memberId) {
        InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        return QuestionResponse.from(question);
    }

    @Transactional
    public CompanyQuestionResponse generateCompanyQuestions(UUID memberId, JobPostingRequest jobPostingRequest) {
        JobPosting jobPosting = jobPostingService.crawlAndSave(jobPostingRequest.url());
        String prompt = promptBuilder.buildCompanyQuestionPrompt(jobPosting.getContent());

        List<QuestionWithTags> aiQuestions = openAiClient.generateQuestions(prompt);

        List<QuestionResponse> questionResponses = aiQuestions.stream()
                .map(aiQuestion -> questionPersistService.saveCompanyQuestion(memberId, aiQuestion, jobPosting))
                .map(QuestionResponse::from)
                .toList();

        log.info("기업 맞춤 질문 생성 완료 - memberId: {}, jobPostingId: {}", memberId, jobPosting.getId());
        return CompanyQuestionResponse.of(JobPostingResponse.from(jobPosting), questionResponses);
    }

    @Transactional
    public List<QuestionResponse> generateVideoQuestions(UUID memberId, VideoInterviewRequest request) {
        MemberSchedulerInfo member = memberServiceClient.getMember(memberId);
        String prompt = promptBuilder.buildVideoQuestionPrompt(member.job(), request.count());

        List<QuestionWithTags> aiQuestions = openAiClient.generateQuestions(prompt);
        InterviewSession session = questionPersistService.createSession(memberId, aiQuestions.size());

        List<QuestionResponse> questionResponses = aiQuestions.stream()
                .map(aiQuestion -> questionPersistService.saveVideoQuestion(memberId, aiQuestion, session))
                .map(QuestionResponse::from)
                .toList();

        log.info("화상 면접 질문 생성 완료 - memberId: {}, sessionId: {}", memberId, session.getId());
        return questionResponses;
    }
}
