package io.wisoft.prepair.prepair_api.service.answer;

import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackDetail;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.AnswerType;
import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.repository.QuestionRepository;
import io.wisoft.prepair.prepair_api.service.stt.SpeechToTextService;
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
    private final QuestionRepository questionRepository;
    private final FileUploader fileUploader;
    private final FeedbackGenerator feedbackGenerator;

    /**
     * S3에서 영상을 다운로드하여 STT 변환 후 피드백을 생성하고 저장한다.
     * 비동기로 실행되며, 실패 시 로그만 남기고 종료한다.
     */
    @Async("videoTaskExecutor")
    public void analyzeSTT(final UUID questionId, final UUID memberId, final String mediaUrl, final String questionTags) {
        try {
            Path videoPath = fileUploader.download(mediaUrl);
            String answer = speechToTextService.convertToTextFromPath(videoPath, questionTags);

            InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

            FeedbackResult result = feedbackGenerator.generate(question, answer);
            FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

            answerPersistService.saveAnswerAndFeedback(
                    questionId, memberId, answer, result, detail, AnswerType.VIDEO, FeedbackType.STT);

            log.info("STT 분석 완료 - questionId: {}", questionId);
        } catch (Exception e) {
            log.error("STT 분석 실패 - questionId: {}", questionId, e);
        }
    }
}
