package io.wisoft.prepair.prepair_api.service.answer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisoft.prepair.prepair_api.dto.AnswerSubmitResult;
import io.wisoft.prepair.prepair_api.dto.CombinedFeedbackResult;
import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackDetail;
import io.wisoft.prepair.prepair_api.entity.InterviewAnswer;
import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.AnswerType;
import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionStatus;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.repository.AnswerRepository;
import io.wisoft.prepair.prepair_api.repository.FeedbackRepository;
import io.wisoft.prepair.prepair_api.repository.QuestionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerPersistenceService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnswerSubmitResult saveAnswerAndFeedback(final UUID questionId, final UUID memberId, final String answer, final FeedbackResult result, final FeedbackDetail detail) {
        InterviewQuestion question = getQuestion(questionId, memberId);
        question.updateStatus(QuestionStatus.ANSWERED);

        InterviewAnswer interviewAnswer = saveAnswer(answer, question);
        InterviewFeedback feedback = saveFeedback(result, detail, interviewAnswer);
        boolean firstAnswer = isFirstTodayAnswer(questionId, feedback.getScore());

        return new AnswerSubmitResult(feedback, firstAnswer);
    }

    private InterviewQuestion getQuestion(UUID questionId, UUID memberId) {
        return questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
    }

    private InterviewAnswer saveAnswer(String answer, InterviewQuestion question) {
        return answerRepository.save(
                new InterviewAnswer(question, answer, AnswerType.TEXT, null)
        );
    }

    private InterviewFeedback saveFeedback(FeedbackResult result, FeedbackDetail detail, InterviewAnswer interviewAnswer) {
        return feedbackRepository.save(
                new InterviewFeedback(interviewAnswer, serializeFeedback(detail), FeedbackType.TEXT, result.score())
        );
    }

    private boolean isFirstTodayAnswer(final UUID questionId, final Integer score) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return questionRepository.updateLatestScoreIfFirstTime(questionId, score, startOfDay, endOfDay) > 0;
    }

    @Transactional
    public InterviewAnswer createVideoAnswer(final UUID questionId, final UUID memberId) {
        InterviewQuestion question = getQuestion(questionId, memberId);
        question.updateStatus(QuestionStatus.ANSWERED);

        return answerRepository.save(
                new InterviewAnswer(question, "", AnswerType.VIDEO, null));
    }

    @Transactional
    public void updateMediaUrl(final UUID answerId, final String mediaUrl) {
        InterviewAnswer interviewAnswer = answerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        interviewAnswer.updateMediaUrl(mediaUrl);
    }

    @Transactional
    public void updateAnswer(final UUID answerId, final String answer) {
        InterviewAnswer interviewAnswer = answerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        interviewAnswer.updateAnswer(answer);
    }

    @Transactional
    public void saveFeedback(final UUID answerId, final FeedbackResult result,
                             final FeedbackDetail detail, final FeedbackType feedbackType) {
        InterviewAnswer interviewAnswer = answerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        feedbackRepository.save(
                new InterviewFeedback(interviewAnswer, serializeFeedback(detail), feedbackType, result.score()));
    }

    @Transactional
    public void saveCombinedFeedback(final UUID answerId, final CombinedFeedbackResult result) {
        InterviewAnswer interviewAnswer = answerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        feedbackRepository.save(
                new InterviewFeedback(interviewAnswer, result.combineFeedback(), FeedbackType.COMBINED, result.score()));
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

