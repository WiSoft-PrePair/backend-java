package io.wisoft.prepair.prepair_api.controller;

import io.wisoft.prepair.prepair_api.dto.request.AnswerRequest;
import io.wisoft.prepair.prepair_api.dto.request.VideoInterviewRequest;
import io.wisoft.prepair.prepair_api.dto.response.FeedbackResponse;
import io.wisoft.prepair.prepair_api.dto.response.QuestionResponse;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import io.wisoft.prepair.prepair_api.global.common.ApiResponse;
import io.wisoft.prepair.prepair_api.service.InterviewService;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/questions")
    public ApiResponse<List<QuestionResponse>> getQuestions(
            @RequestHeader("X-User-Id") UUID memberId,
            @RequestParam QuestionType type
    ) {
        List<QuestionResponse> data = interviewService.getQuestions(memberId, type)
                .stream()
                .map(QuestionResponse::from)
                .toList();

        return ApiResponse.ok(data, "질문 목록을 조회했습니다.");
    }

    @GetMapping("/questions/{questionId}")
    public ApiResponse<QuestionResponse> getQuestion(
            @PathVariable UUID questionId,
            @RequestHeader("X-User-Id") UUID memberId
    ) {
        QuestionResponse data = QuestionResponse.from(interviewService.getQuestion(questionId, memberId));
        return ApiResponse.ok(data, "특정 질문을 조회했습니다.");
    }


    @PostMapping("/questions/{questionId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FeedbackResponse> submitAnswer(
            @PathVariable UUID questionId,
            @RequestHeader("X-User-Id") UUID memberId,
            @RequestBody @Valid AnswerRequest request
    ) {
        FeedbackResponse data = interviewService.submitAnswer(questionId, memberId, request.answer());
        return ApiResponse.created(data, "답변이 제출되었습니다.");
    }

    @PostMapping(value = "/questions/{questionId}/video-answers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> submitVideoAnswer(
            @PathVariable UUID questionId,
            @RequestHeader("X-User-Id") UUID memberId,
            @RequestPart("video") MultipartFile video
    ) {
        interviewService.submitVideoAnswer(questionId, memberId, video);
        return ApiResponse.accepted(null, "영상 답변이 제출되었습니다.");
    }

    @PostMapping("/questions/video")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<QuestionResponse>> generateVideoQuestion(
            @RequestHeader("X-User-Id") UUID memberId,
            @RequestBody @Valid VideoInterviewRequest request
    ) {
        List<QuestionResponse> data = interviewService.generateVideoQuestions(memberId, request)
                .stream()
                .map(QuestionResponse::from)
                .toList();

        return ApiResponse.created(data, "화상 면접 질문이 생성되었습니다.");
    }
}

