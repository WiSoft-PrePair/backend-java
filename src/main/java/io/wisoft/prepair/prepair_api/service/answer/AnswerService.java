package io.wisoft.prepair.prepair_api.service.answer;

import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackDetail;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.AnswerType;
import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.repository.QuestionRepository;
import io.wisoft.prepair.prepair_api.storage.FileUploader;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerPersistService answerPersistService;
    private final VideoAnswerAnalyzer videoAnswerAnalyzer;
    private final FeedbackGenerator feedbackGenerator;
    private final QuestionRepository questionRepository;
    private final MemberServiceClient memberServiceClient;
    private final FileUploader fileUploader;

    public FeedbackResponse submitAnswer(final UUID questionId, final UUID memberId, final String answer) {
        InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        FeedbackResult result = feedbackGenerator.generate(question, answer);
        FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

        InterviewFeedback feedback = answerPersistService.saveAnswerAndFeedback(
                questionId, memberId, answer, result, detail, AnswerType.TEXT, FeedbackType.TEXT);

        log.info("답변 제출 및 피드백 생성 완료 - questionId: {}, score: {}", questionId, result.score());
        return FeedbackResponse.from(feedback, detail);
    }

    public void submitVideoAnswer(final UUID questionId, final UUID memberId, final MultipartFile video) {
        InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        String email = memberServiceClient.getMember(memberId).email();
        String mediaUrl = fileUploader.upload(video, email);

        videoAnswerAnalyzer.analyzeSTT(questionId, memberId, mediaUrl, question.getQuestionTag());

        log.info("영상 답변 처리 완료 - questionId: {}, mediaUrl: {}", questionId, mediaUrl);
    }
}
