package io.wisoft.prepair.prepair_api.service.answer;

import io.wisoft.prepair.prepair_api.dto.AnswerSubmitResult;
import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackDetail;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewAnswer;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerPersistenceService answerPersistenceService;
    private final VideoAnswerAnalyzer videoAnswerAnalyzer;
    private final FeedbackGenerator feedbackGenerator;
    private final QuestionRepository questionRepository;
    private final MemberServiceClient memberServiceClient;
    private final AnalysisCompletionTracker completionTracker;

    public FeedbackResponse submitAnswer(final UUID questionId, final UUID memberId, final String answer) {
        // AI 피드백 생성
        InterviewQuestion question = getQuestion(questionId, memberId);
        FeedbackResult result = feedbackGenerator.generate(question, answer);
        FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

        // 답변 + 피드백 저장 (최초 답변 여부 원자적 판단 포함)
        AnswerSubmitResult answerSubmitResult = answerPersistenceService.saveAnswerAndFeedback(
                questionId, memberId, answer, result, detail
        );

        log.info("답변 제출 및 피드백 생성 완료 - questionId: {}, score: {}", questionId, result.score());

        // 오늘의 질문 최초 답변 시 점수 전송 (트랜잭션 외부)
        if (answerSubmitResult.firstAnswer()) {
            memberServiceClient.sendScore(memberId, answerSubmitResult.feedback().getScore());
        }

        return FeedbackResponse.from(answerSubmitResult.feedback(), detail);
    }

    public void submitVideoAnswer(final UUID questionId, final UUID memberId, final MultipartFile video) {
        // S3 업로드에 필요한 이메일 조회
        String email = memberServiceClient.getMember(memberId).email();

        // 임시 파일 먼저 생성 후 DB 저장 (실패 시 고아 레코드 방지)
        Path videoPath = createTempFile(video);
        InterviewAnswer answer = answerPersistenceService.createVideoAnswer(questionId, memberId);

        // 3개 비동기 작업 모두 완료 시 이벤트 발행을 위한 트래커 초기화
        completionTracker.init(answer.getId(), videoPath);
        log.info("영상 답변 분석 시작 - questionId: {}, answerId: {}", questionId, answer.getId());

        videoAnswerAnalyzer.uploadToS3(answer.getId(), videoPath, video.getContentType(), email);
        videoAnswerAnalyzer.analyzeSTT(answer.getId(), questionId, memberId, videoPath);
        videoAnswerAnalyzer.analyzeVideo(answer.getId(), videoPath);
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
