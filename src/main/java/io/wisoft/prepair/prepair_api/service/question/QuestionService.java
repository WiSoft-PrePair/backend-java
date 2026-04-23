package io.wisoft.prepair.prepair_api.service.question;

import io.wisoft.prepair.prepair_api.dto.request.VideoInterviewRequest;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.InterviewSession;
import io.wisoft.prepair.prepair_api.entity.JobPosting;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.global.client.member.dto.MemberSchedulerInfo;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.client.openai.dto.QuestionWithTags;
import io.wisoft.prepair.prepair_api.prompt.PromptBuilder;
import io.wisoft.prepair.prepair_api.repository.QuestionRepository;
import io.wisoft.prepair.prepair_api.repository.SessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final SessionRepository sessionRepository;
    private final QuestionPersistService interviewQuestionService;
    private final MemberServiceClient memberServiceClient;
    private final OpenAiClient openAiClient;
    private final PromptBuilder promptBuilder;

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

    public List<InterviewQuestion> generateVideoQuestions(UUID memberId, VideoInterviewRequest request) {
        MemberSchedulerInfo member = memberServiceClient.getMember(memberId);
        String prompt = promptBuilder.buildVideoQuestionPrompt(member.job(), request.count());
        List<QuestionWithTags> results = openAiClient.generateQuestions(prompt);

        InterviewSession session = sessionRepository.save(new InterviewSession(memberId, request.count()));

        List<InterviewQuestion> questions = results.stream()
                .map(result -> interviewQuestionService.saveVideoQuestion(memberId, result, session))
                .toList();

        log.info("화상 면접 질문 생성 완료 - memberId: {}, sessionId: {}", memberId, session.getId());
        return questions;
    }

    public void validateSessionOwner(UUID sessionId, UUID memberId) {
        if(!sessionRepository.existsByIdAndMemberId(sessionId, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
