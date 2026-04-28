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
@Transactional
public class AnswerPersistenceService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper;

    public AnswerSubmitResult saveAnswerAndFeedback(final UUID questionId, final UUID memberId, final String answerText, final FeedbackResult result, final FeedbackDetail detail) {
        InterviewQuestion question = getQuestion(questionId, memberId);
        question.updateStatus(QuestionStatus.ANSWERED);

        // 답변 + 피드백 저장
        InterviewAnswer answer = saveAnswer(answerText, question);
        InterviewFeedback feedback = saveTextFeedback(result, detail, answer);

        // 조건부 UPDATE로 최초 답변 여부 원자적 판단 (race condition 방지)
        boolean firstAnswer = isFirstTodayAnswer(questionId, feedback.getScore());

        // sendScore는 트랜잭션 외부에서 호출하므로 firstAnswer를 함께 반환
        return new AnswerSubmitResult(feedback, firstAnswer);
    }

    public InterviewAnswer saveVideoAnswer(final UUID questionId, final UUID memberId) {
        InterviewQuestion question = getQuestion(questionId, memberId);
        question.updateStatus(QuestionStatus.ANSWERED);
        return answerRepository.save(new InterviewAnswer(question, "", AnswerType.VIDEO, null));
    }

    public void updateMediaUrl(final UUID answerId, final String mediaUrl) {
        InterviewAnswer interviewAnswer = getAnswer(answerId);
        interviewAnswer.updateMediaUrl(mediaUrl);
    }

    public void updateAnswer(final UUID answerId, final String answer) {
        InterviewAnswer interviewAnswer = getAnswer(answerId);
        interviewAnswer.updateAnswer(answer);
    }

    public void saveVideoFeedback(final UUID answerId, final FeedbackResult result, final FeedbackDetail detail, final FeedbackType feedbackType) {
        InterviewAnswer interviewAnswer = getAnswer(answerId);
        feedbackRepository.save(new InterviewFeedback(interviewAnswer, serializeFeedback(detail), feedbackType, result.score()));
    }

    public void saveCombinedFeedback(final UUID answerId, final CombinedFeedbackResult result) {
        InterviewAnswer interviewAnswer = getAnswer(answerId);
        feedbackRepository.save(new InterviewFeedback(interviewAnswer, result.combineFeedback(), FeedbackType.COMBINED, result.score()));
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

    private InterviewFeedback saveTextFeedback(FeedbackResult result, FeedbackDetail detail, InterviewAnswer interviewAnswer) {
        return feedbackRepository.save(
                new InterviewFeedback(interviewAnswer, serializeFeedback(detail), FeedbackType.TEXT, result.score())
        );
    }

    private boolean isFirstTodayAnswer(final UUID questionId, final Integer score) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return questionRepository.updateLatestScoreIfFirstTime(questionId, score, startOfDay, endOfDay) > 0;
    }

    private InterviewAnswer getAnswer(UUID answerId) {
        return answerRepository.findById(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
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

