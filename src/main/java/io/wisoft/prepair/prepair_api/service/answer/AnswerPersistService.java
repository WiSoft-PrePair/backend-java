package io.wisoft.prepair.prepair_api.service.answer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackDetail;
import io.wisoft.prepair.prepair_api.entity.InterviewAnswer;
import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.AnswerType;
import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionStatus;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.repository.AnswerRepository;
import io.wisoft.prepair.prepair_api.repository.FeedbackRepository;
import io.wisoft.prepair.prepair_api.repository.QuestionRepository;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnswerPersistService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final MemberServiceClient memberServiceClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public InterviewFeedback saveAnswerAndFeedback(
            final UUID questionId, final UUID memberId, final String answer,
            final FeedbackResult result, final FeedbackDetail detail,
            final AnswerType answerType, final FeedbackType feedbackType
    ) {
        InterviewQuestion question = getQuestion(questionId, memberId);
        question.updateStatus(QuestionStatus.ANSWERED);

        InterviewAnswer interviewAnswer = answerRepository.save(
                new InterviewAnswer(question, answer, answerType, null)
        );

        InterviewFeedback feedback = feedbackRepository.save(
                new InterviewFeedback(interviewAnswer, serializeFeedback(detail), feedbackType, result.score())
        );

        if (question.isTodayQuestionFirstAnswer()) {
            memberServiceClient.sendScore(memberId, result.score());
        }

        question.updateLatestScore(result.score());

        return feedback;
    }

    @Transactional
    public void saveVideoAnalysisFeedback(
            final UUID questionId, final UUID memberId, final String answer,
            final FeedbackResult sttResult, final FeedbackDetail sttDetail,
            final String mediaUrl, final FeedbackResult videoResult,
            final FeedbackDetail videoDetail
    ) {
        InterviewQuestion question = getQuestion(questionId, memberId);
        question.updateStatus(QuestionStatus.ANSWERED);

        InterviewAnswer interviewAnswer = answerRepository.save(
                new InterviewAnswer(question, answer, AnswerType.VIDEO, mediaUrl));

        // STT 피드백
        feedbackRepository.save(new InterviewFeedback(interviewAnswer, serializeFeedback(sttDetail), FeedbackType.STT,
                sttResult.score()));

        // 비디오 분석 피드백
        feedbackRepository.save(
                new InterviewFeedback(interviewAnswer, serializeFeedback(videoDetail), FeedbackType.VIDEO,
                        videoResult.score()));

        log.info("비디오 분석 피드백 저장 완료 - answerId: {}", interviewAnswer.getId());
    }

    private InterviewQuestion getQuestion(UUID questionId, UUID memberId) {
        return questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
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
