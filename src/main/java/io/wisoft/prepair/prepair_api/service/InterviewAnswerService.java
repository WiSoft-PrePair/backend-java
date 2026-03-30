package io.wisoft.prepair.prepair_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackDetail;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewFeedback;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.AnswerType;
import io.wisoft.prepair.prepair_api.entity.enums.FeedbackType;
import io.wisoft.prepair.prepair_api.global.client.member.MemberServiceClient;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import io.wisoft.prepair.prepair_api.global.exception.BusinessException;
import io.wisoft.prepair.prepair_api.global.exception.ErrorCode;
import io.wisoft.prepair.prepair_api.prompt.InterviewPromptBuilder;
import io.wisoft.prepair.prepair_api.repository.InterviewQuestionRepository;
import io.wisoft.prepair.prepair_api.storage.FileUploader;
import io.wisoft.prepair.prepair_api.video.service.VideoAnalysisService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAnswerService {

    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerPersistService persistService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final MemberServiceClient memberServiceClient;
    private final InterviewPromptBuilder promptBuilder;
    private final SpeechToTextService speechToTextService;
    private final FileUploader fileUploader;
    private final VideoAnalysisService videoAnalysisService;

    public FeedbackResponse submitAnswer(final UUID questionId, final UUID memberId, final String answer) {
        InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        FeedbackResult result = generateTextFeedback(question, answer);
        FeedbackDetail detail = new FeedbackDetail(result.good(), result.improvement(), result.recommendation());

        InterviewFeedback feedback = persistService.saveAnswerAndFeedback(
                questionId, memberId, answer, result, detail, AnswerType.TEXT, FeedbackType.TEXT );

        log.info("답변 제출 및 피드백 생성 완료 - questionId: {}, score: {}", questionId, result.score());
        return FeedbackResponse.from(feedback, detail);
    }

    public void submitVideoAnswer(final UUID questionId, final UUID memberId, final MultipartFile video) {
        InterviewQuestion question = questionRepository.findByIdAndMemberId(questionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        //RustFS 영상 저장
        String email = memberServiceClient.getMember(memberId).email();
        String videoUrl = fileUploader.upload(video, email);

        //STT 피드백
        String answer = speechToTextService.transcribe(video, question.getQuestionTag());
        FeedbackResult sttResult = generateTextFeedback(question, answer);
        FeedbackDetail sttDetail = new FeedbackDetail(sttResult.good(), sttResult.improvement(), sttResult.recommendation());

        //비디오 분석 피드백
        FeedbackResult videoResult = videoAnalysisService.analyze(video, question.getQuestion());
        FeedbackDetail videoDetail = new FeedbackDetail(videoResult.good(), videoResult.improvement(), videoResult.recommendation());

        //STT, 비디오 피드백 저장
        persistService.saveVideoAnalysisFeedback(questionId, memberId, answer, sttResult, sttDetail, videoUrl, videoResult, videoDetail);

        log.info("영상 답변 처리 완료 - questionId: {}, videoUrl: {}", questionId, videoUrl);
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
}