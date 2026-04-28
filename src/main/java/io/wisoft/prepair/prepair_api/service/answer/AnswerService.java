package io.wisoft.prepair.prepair_api.service.answer;

import io.wisoft.prepair.prepair_api.dto.AnswerSubmitResult;
import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackDetail;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewAnswer;
import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.repository.QuestionRepository;
import io.wisoft.prepair.prepair_api.service.answer.event.AnalysisCompletionTracker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerPersistenceService answerPersistenceService;
    private final VideoAnswerAnalyzer videoAnswerAnalyzer;
    private final VideoAnswerStreamService videoAnswerStreamService;
    private final FeedbackGenerator feedbackGenerator;
    private final QuestionRepository questionRepository;
    private final MemberServiceClient memberServiceClient;
    private final AnalysisCompletionTracker completionTracker;

    public FeedbackResponse submitAnswer(final UUID questionId, final UUID memberId, final String answer) {
        InterviewQuestion question = getQuestion(questionId, memberId);
        FeedbackResult result = feedbackGenerator.generate(question, answer);
        FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

        AnswerSubmitResult answerSubmitResult = answerPersistenceService.saveAnswerAndFeedback(
                questionId, memberId, answer, result, detail
        );

        if (answerSubmitResult.firstAnswer()) {
            memberServiceClient.sendScore(memberId, answerSubmitResult.feedback().getScore());
        }

        log.info("답변 제출 및 피드백 생성 완료 - questionId: {}, score: {}", questionId, result.score());
        return FeedbackResponse.from(answerSubmitResult.feedback(), detail);
    }

    public void submitVideoAnswer(final UUID questionId, final UUID memberId, final MultipartFile video) {
        InterviewQuestion question = getQuestion(questionId, memberId);

        String email = memberServiceClient.getMember(memberId).email();
        InterviewAnswer answer = answerPersistenceService.createVideoAnswer(questionId, memberId);
        Path videoPath = createTempFile(video);

        completionTracker.init(answer.getId(), videoPath);

        videoAnswerAnalyzer.uploadToS3(answer.getId(), videoPath, video.getContentType(), email);
        videoAnswerAnalyzer.analyzeSTT(answer.getId(), questionId, memberId, videoPath, question.getQuestionTag());
        videoAnswerAnalyzer.analyzeVideo(answer.getId(), videoPath);
    }

    public SseEmitter subscribeSession(final UUID sessionId, final UUID memberId) {
        return videoAnswerStreamService.subscribe(sessionId, memberId);
    }

    private InterviewQuestion getQuestion(UUID questionId, UUID memberId) {
        return questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
    }

    private Path createTempFile(final MultipartFile video) {
        try {
            Path videoPath = Files.createTempFile("video-", getExtension(video.getOriginalFilename()));
            video.transferTo(videoPath);
            return videoPath;
        } catch (Exception e) {
            log.error("영상 임시 파일 생성 실패 - filename: {}", video.getOriginalFilename(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String getExtension(final String filename) {
        if (filename == null || filename.isBlank()) {
            return ".tmp";
        }

        int extensionIndex = filename.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == filename.length() - 1) {
            return ".tmp";
        }

        return filename.substring(extensionIndex);
    }
}
