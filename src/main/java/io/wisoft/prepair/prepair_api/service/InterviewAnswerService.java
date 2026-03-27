package io.wisoft.prepair.prepair_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackDetail;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewAnswer;
import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.AnswerType;
import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionStatus;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.prompt.InterviewPromptBuilder;
import io.wisoft.prepair.prepair_api.repository.InterviewAnswerRepository;
import io.wisoft.prepair.prepair_api.repository.InterviewFeedbackRepository;
import io.wisoft.prepair.prepair_api.repository.InterviewQuestionRepository;
import io.wisoft.prepair.prepair_api.dto.FeedbackResult;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final SpeechToTextService speechToTextService;

    @Lazy
    @Autowired
    private InterviewAnswerService self;

    public FeedbackResponse submitAnswer(final UUID questionId, final UUID memberId, final String answer) {
        InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        FeedbackResult result = generateTextFeedback(question, answer);
        FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

        InterviewFeedback feedback = self.persistAnswerAndFeedback(
                questionId, memberId, answer, result, detail, AnswerType.TEXT, FeedbackType.TEXT);

        log.info("답변 제출 및 피드백 생성 완료 - questionId: {}, score: {}", questionId, result.score());
        return FeedbackResponse.from(feedback, detail);
    }

    public void submitVideoAnswer(final UUID questionId, final UUID memberId, final MultipartFile video) {
        InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        String answer = speechToTextService.transcribe(video, question.getQuestionTag());
        FeedbackResult result = generateTextFeedback(question, answer);
        FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

        self.persistAnswerAndFeedback(questionId, memberId, answer, result, detail, AnswerType.VIDEO, FeedbackType.STT);

        log.info("STT 피드백 생성 완료 - questionId: {}, score: {}", questionId, result.score());
    }

    @Transactional
    public InterviewFeedback persistAnswerAndFeedback(
            final UUID questionId, final UUID memberId, final String answer,
            final FeedbackResult result, final FeedbackDetail detail,
            final AnswerType answerType, final FeedbackType feedbackType
    ) {
        InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 1. 답변 상태 저장
        question.updateStatus(QuestionStatus.ANSWERED);

        // 2. 답변 저장
        InterviewAnswer interviewAnswer = answerRepository.save(
                new InterviewAnswer(question, answer, answerType, null)
        );

        // 3. 피드백 저장
        InterviewFeedback feedback = feedbackRepository.save(
                new InterviewFeedback(interviewAnswer, serializeFeedback(detail), feedbackType, result.score())
        );

        // 4. 적립급 여부 판별
        if (question.isTodayQuestionFirstAnswer()) {
            memberServiceClient.sendScore(memberId, result.score());
        }

        // 5. 점수 업데이트
        question.updateLatestScore(result.score());

        return feedback;
    }

    private FeedbackResult generateTextFeedback(final InterviewQuestion question, final String answer) {
        String prompt = promptBuilder.buildFeedbackPrompt(question.getQuestion(), question.getQuestionTag(), answer);
        return parseFeedback(openAiClient.generateText(prompt));
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