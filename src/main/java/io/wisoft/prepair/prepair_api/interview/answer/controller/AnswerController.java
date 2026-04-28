package io.wisoft.prepair.prepair_api.interview.answer.controller;

import io.wisoft.prepair.prepair_api.interview.answer.dto.AnswerRequest;
import io.wisoft.prepair.prepair_api.interview.answer.dto.FeedbackResponse;
import io.wisoft.prepair.prepair_api.common.response.ApiResponse;
import io.wisoft.prepair.prepair_api.interview.answer.service.AnswerService;
import io.wisoft.prepair.prepair_api.interview.answer.service.VideoAnswerSseService;
import jakarta.validation.Valid;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interviews/questions")
public class AnswerController {

    private final AnswerService answerService;
    private final VideoAnswerSseService videoAnswerStreamService;

    @PostMapping("/{questionId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FeedbackResponse> submitAnswer(
            @PathVariable UUID questionId,
            @RequestHeader("X-User-Id") UUID memberId,
            @RequestBody @Valid AnswerRequest request
    ) {
        FeedbackResponse data = answerService.submitAnswer(questionId, memberId, request.answer());
        return ApiResponse.created(data, "답변이 제출되었습니다.");
    }

    @PostMapping(value = "/{questionId}/video-answers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> submitVideoAnswer(
            @PathVariable UUID questionId,
            @RequestHeader("X-User-Id") UUID memberId,
            @RequestPart("video") MultipartFile video
    ) {
        answerService.submitVideoAnswer(questionId, memberId, video);
        return ApiResponse.accepted(null, "영상 답변이 제출되었습니다.");
    }

    @GetMapping(value = "/video-answers/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID memberId
    ) {
        return videoAnswerStreamService.subscribe(sessionId, memberId);
    }
}
