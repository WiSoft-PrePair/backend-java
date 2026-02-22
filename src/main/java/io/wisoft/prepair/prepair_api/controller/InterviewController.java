package io.wisoft.prepair.prepair_api.controller;

import io.wisoft.prepair.prepair_api.controller.dto.response.QuestionResponse;
import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.entity.enums.QuestionType;
import io.wisoft.prepair.prepair_api.global.common.ApiResponse;
import io.wisoft.prepair.prepair_api.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("me/questions")
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
}

