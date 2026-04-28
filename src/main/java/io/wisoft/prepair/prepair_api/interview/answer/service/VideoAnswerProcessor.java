package io.wisoft.prepair.prepair_api.interview.answer.service;

import io.wisoft.prepair.prepair_api.interview.answer.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.interview.answer.dto.FeedbackDetail;
import io.wisoft.prepair.prepair_api.interview.question.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.interview.answer.entity.FeedbackType;
import io.wisoft.prepair.prepair_api.common.exception.BusinessException;
import io.wisoft.prepair.prepair_api.common.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.interview.question.repository.QuestionRepository;
import io.wisoft.prepair.prepair_api.interview.answer.event.AnalysisCompletionTracker;
import io.wisoft.prepair.prepair_api.external.storage.FileUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAnswerProcessor {

    private final AnswerPersistenceService answerPersistenceService;
    private final SpeechToTextService speechToTextService;
    private final VideoFrameAnalysisService videoAnalysisService;
    private final QuestionRepository questionRepository;
    private final FileUploader fileUploader;
    private final FeedbackGenerator feedbackGenerator;
    private final AnalysisCompletionTracker completionTracker;

    @Async("videoTaskExecutor")
    public void uploadToS3(final UUID answerId, final Path videoPath, final String contentType, final String email) {
        try {
            String mediaUrl = fileUploader.upload(videoPath, contentType, email);
            answerPersistenceService.updateMediaUrl(answerId, mediaUrl);
            log.info("[VIDEO-S3] 업로드 완료 - answerId: {}", answerId);
            completionTracker.complete(answerId);
        } catch (Exception e) {
            log.error("[VIDEO-S3] 업로드 실패 - answerId: {}, error: {}", answerId, e.getMessage(), e);
            completionTracker.fail(answerId);
        }
    }

    @Async("videoTaskExecutor")
    public void analyzeSTT(final UUID answerId, final UUID questionId, final UUID memberId, final Path videoPath) {
        try {
            InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

            String answer = speechToTextService.convertToTextFromPath(videoPath, question.getQuestionTag());
            answerPersistenceService.updateAnswer(answerId, answer);

            FeedbackResult result = feedbackGenerator.generate(question, answer);
            FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

            answerPersistenceService.saveVideoFeedback(answerId, result, detail, FeedbackType.STT);

            log.info("[VIDEO-STT] 분석 완료 - answerId: {}", answerId);
            completionTracker.complete(answerId);
        } catch (Exception e) {
            log.error("[VIDEO-STT] 분석 실패 - answerId: {}, error: {}", answerId, e.getMessage(), e);
            completionTracker.fail(answerId);
        }
    }

    @Async("videoTaskExecutor")
    public void analyzeVideo(final UUID answerId, final Path videoPath) {
        try {
            FeedbackResult result = videoAnalysisService.analyze(videoPath);
            FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

            answerPersistenceService.saveVideoFeedback(answerId, result, detail, FeedbackType.VIDEO);

            log.info("[VIDEO-ANALYSIS] 분석 완료 - answerId: {}", answerId);
            completionTracker.complete(answerId);
        } catch (Exception e) {
            log.error("[VIDEO-ANALYSIS] 분석 실패 - answerId: {}, error: {}", answerId, e.getMessage(), e);
            completionTracker.fail(answerId);
        }
    }
}
