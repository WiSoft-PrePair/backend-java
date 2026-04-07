package io.wisoft.prepair.prepair_api.service.answer;

import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackDetail;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.repository.QuestionRepository;
import io.wisoft.prepair.prepair_api.service.answer.event.AnalysisCompletionTracker;
import io.wisoft.prepair.prepair_api.service.stt.SpeechToTextService;
import io.wisoft.prepair.prepair_api.service.vidoe.VideoAnalysisService;
import io.wisoft.prepair.prepair_api.storage.FileUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoAnswerAnalyzer {

    private final AnswerPersistService answerPersistService;
    private final SpeechToTextService speechToTextService;
    private final VideoAnalysisService videoAnalysisService;
    private final QuestionRepository questionRepository;
    private final FileUploader fileUploader;
    private final FeedbackGenerator feedbackGenerator;
    private final AnalysisCompletionTracker completionTracker;

    @Async("videoTaskExecutor")
    public void uploadToS3(final UUID answerId, final Path videoPath, final String contentType, final String email) {
        try {
            String mediaUrl = fileUploader.upload(videoPath, contentType, email);
            answerPersistService.updateMediaUrl(answerId, mediaUrl);
            log.info("[VIDEO-S3] 업로드 완료 - answerId: {}", answerId);
            completionTracker.complete(answerId);
        } catch (Exception e) {
            log.error("[VIDEO-S3] 업로드 실패 - answerId: {}, error: {}", answerId, e.getMessage(), e);
            completionTracker.fail(answerId);
        }
    }

    @Async("videoTaskExecutor")
    public void analyzeSTT(final UUID answerId, final UUID questionId, final UUID memberId,
                           final Path videoPath, final String questionTags) {
        try {
            String answer = speechToTextService.convertToTextFromPath(videoPath, questionTags);
            answerPersistService.updateAnswer(answerId, answer);

            InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

            FeedbackResult result = feedbackGenerator.generate(question, answer);
            FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

            answerPersistService.saveFeedback(answerId, result, detail, FeedbackType.STT);
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

            answerPersistService.saveFeedback(answerId, result, detail, FeedbackType.VIDEO);
            log.info("[VIDEO-ANALYSIS] 분석 완료 - answerId: {}", answerId);
            completionTracker.complete(answerId);
        } catch (Exception e) {
            log.error("[VIDEO-ANALYSIS] 분석 실패 - answerId: {}, error: {}", answerId, e.getMessage(), e);
            completionTracker.fail(answerId);
        }
    }
}
