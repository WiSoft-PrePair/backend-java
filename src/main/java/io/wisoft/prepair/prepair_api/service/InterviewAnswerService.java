package io.wisoft.prepair.prepair_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisoft.prepair.prepair_api.controller.dto.response.FeedbackDetail;
import io.wisoft.prepair.prepair_api.controller.dto.response.FeedbackResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewAnswer;
import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.AnswerType;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionStatus;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.prompt.InterviewPromptBuilder;
import io.wisoft.prepair.prepair_api.repository.InterviewAnswerRepository;
import io.wisoft.prepair.prepair_api.repository.InterviewFeedbackRepository;
import io.wisoft.prepair.prepair_api.repository.InterviewQuestionRepository;
import io.wisoft.prepair.prepair_api.service.dto.FeedbackResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAnswerService {

    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final OpenAiClient openAiClient;
    private final InterviewPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final MemberServiceClient memberServiceClient;

    @Lazy
    @Autowired
    private InterviewAnswerService self;

    public FeedbackResponse submitAnswer(final UUID questionId, final UUID memberId, final String answer, final AnswerType answerType, final String mediaUrl) {
        InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        FeedbackResult result = generateFeedback(question, answer, answerType, mediaUrl);
        FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

        InterviewFeedback feedback = self.persistAnswerAndFeedback(questionId, memberId, answer, answerType, mediaUrl, result, detail);

        log.info("답변 제출 및 피드백 생성 완료 - questionId: {}, score: {}", questionId, result.score());
        return FeedbackResponse.from(feedback, detail);
    }

    @Transactional
    public InterviewFeedback persistAnswerAndFeedback(final UUID questionId, final UUID memberId, final String answer, final AnswerType answerType, final String mediaUrl, final FeedbackResult result, final FeedbackDetail detail
    ) {
        InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        InterviewAnswer interviewAnswer = saveAnswer(question, answer, answerType, mediaUrl);

        InterviewFeedback feedback = feedbackRepository.save(
                new InterviewFeedback(interviewAnswer, serializeFeedback(detail), result.score())
        );

        boolean isFirstFeedback = question.getLatestScore() == null;
        question.updateLatestScore(result.score());

        if (isFirstFeedback) {
            memberServiceClient.sendScore(memberId, result.score());
        }
        return feedback;
    }


    private InterviewAnswer saveAnswer(final InterviewQuestion question, final String answer, final AnswerType answerType, final String mediaUrl) {
        question.updateStatus(QuestionStatus.ANSWERED);
        return answerRepository.save(new InterviewAnswer(question, answer, answerType, mediaUrl));
    }

    private FeedbackResult generateFeedback(final InterviewQuestion question, final String answer, final AnswerType answerType, final String mediaUrl) {
        return switch (answerType) {
            case TEXT -> generateTextFeedback(question.getQuestion(), question.getQuestionTag(), answer);
            case VIDEO -> generateVideoFeedback(question.getQuestion(), answer, mediaUrl);
        };
    }

    private FeedbackResult generateTextFeedback(final String question, final String questionTag, final String answer) {
        String prompt = promptBuilder.buildFeedbackPrompt(question, questionTag, answer);
        return parseFeedback(openAiClient.generateText(prompt));
    }

    private FeedbackResult generateVideoFeedback(final String question, final String answer, final String mediaUrl) {
        // TODO: 1. mediaUrl에서 텍스트 추출 (STT)
        //       2. 텍스트 + 영상 분석 프롬프트 빌드
        //       3. 피드백 생성
        throw new UnsupportedOperationException("영상 분석 미구현");
    }

    private FeedbackResult parseFeedback(final String raw) {
        try {
            return objectMapper.readValue(raw, FeedbackResult.class);
        } catch (JsonProcessingException e) {
            log.error("피드백 응답 파싱 실패: {}", raw, e);
            throw new BusinessException(ErrorCode.OPENAI_RESPONSE_PARSE_ERROR);
        }
    }

    private String serializeFeedback(final FeedbackDetail detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            log.error("피드백 직렬화 실패", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}