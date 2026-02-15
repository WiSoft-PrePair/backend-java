package io.wisoft.prepair.prepair_api.controller;

import io.wisoft.prepair.prepair_api.dto.response.TodayQuestionResponse;
import io.wisoft.prepair.prepair_api.global.common.ApiResponse;
import io.wisoft.prepair.prepair_api.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/me/today")
    public ResponseEntity<ApiResponse<TodayQuestionResponse>> createTodayQuestion(
            @RequestHeader("X-User-Id") String userId
    ) {
        log.info("오늘의 질문 생성 요청 - userId: {}", userId);

        TodayQuestionResponse data = interviewService.createTodayQuestion(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(data, "오늘의 질문이 생성되었습니다."));
    }
}
