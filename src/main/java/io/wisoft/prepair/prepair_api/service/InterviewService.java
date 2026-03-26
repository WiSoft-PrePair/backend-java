package io.wisoft.prepair.prepair_api.service;

import io.wisoft.prepair.prepair_api.dto.request.VideoInterviewRequest;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.JobPosting;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.global.client.member.dto.MemberSchedulerInfo;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.prompt.InterviewPromptBuilder;
import io.wisoft.prepair.prepair_api.repository.InterviewQuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewQuestionRepository questionRepository;
    private final InterviewQuestionService interviewQuestionService;
    private final InterviewAnswerService interviewAnswerService;
    private final MemberServiceClient memberServiceClient;
    private final OpenAiClient openAiClient;
    private final InterviewPromptBuilder promptBuilder;

    @Transactional(readOnly = true)
    public List<InterviewQuestion> getQuestions(UUID memberId, QuestionType type) {
        return questionRepository.findByMemberIdAndQuestionTypeOrderByCreatedAtDesc(memberId, type);
    }

    @Transactional(readOnly = true)
    public InterviewQuestion getQuestion(UUID questionId, UUID memberId) {
        return questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
    }

    public List<InterviewQuestion> generateCompanyQuestions(UUID memberId, JobPosting jobPosting) {
        try {
            final String prompt = promptBuilder.buildCompanyQuestionPrompt(jobPosting.getContent());
            final List<QuestionWithTags> results = openAiClient.generateQuestions(prompt);
            final List<InterviewQuestion> questions = results.stream()
                    .map(result ->
                            interviewQuestionService.saveCompanyQuestion(memberId, result, jobPosting))
                    .toList();
            log.info("기업 맞춤 질문 생성 완료 - memberId: {}, jobPostingId: {}", memberId, jobPosting.getId());

            return questions;
        } catch (Exception e) {
            log.error("기업 맞춤 질문 생성 실패 - memberId: {}, jobPostingId: {}", memberId, jobPosting.getId(), e);
            throw e;
        }
    }

    public FeedbackResponse submitAnswer(UUID questionId, UUID memberId, String answer) {
        return interviewAnswerService.submitAnswer(questionId, memberId, answer);
    }

    public List<InterviewQuestion> generateVideoQuestions(UUID memberId, VideoInterviewRequest request) {
        MemberSchedulerInfo member = memberServiceClient.getMember(memberId);
        String prompt = promptBuilder.buildVideoQuestionPrompt(member.job(), request.count());
        List<QuestionWithTags> results = openAiClient.generateQuestions(prompt);

        List<InterviewQuestion> questions = results.stream()
                .map(result -> interviewQuestionService.saveVideoQuestion(memberId, result))
                .toList();

        log.info("화상 면접 질문 생성 완료 - memberId: {}", memberId);
        return questions;
    }

    public void submitVideoAnswer(final UUID questionId, final UUID memberId, final MultipartFile video) {
        interviewAnswerService.submitVideoAnswer(questionId, memberId, video);
    }
}
