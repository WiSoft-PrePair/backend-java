package io.wisoft.prepair.prepair_api.interview.session.controller;

import io.wisoft.prepair.prepair_api.common.response.ApiResponse;
import io.wisoft.prepair.prepair_api.interview.session.dto.response.CreateSessionResponse;
import io.wisoft.prepair.prepair_api.interview.session.dto.response.SessionDetailResponse;
import io.wisoft.prepair.prepair_api.interview.session.dto.response.SessionResponse;
import io.wisoft.prepair.prepair_api.interview.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interviews/sessions")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ApiResponse<List<SessionResponse>> getSessions(
            @RequestHeader("X-User-Id") UUID memberId
    ) {
        List<SessionResponse> data = sessionService.getSessionsByMember(memberId);
        return ApiResponse.ok(data, "세션 목록을 조회했습니다.");
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<SessionDetailResponse> getSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID memberId
    ) {
        SessionDetailResponse data = sessionService.getSessionDetail(sessionId, memberId);
        return ApiResponse.ok(data, "세션 결과를 조회했습니다.");
    }

    @PostMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateSessionResponse> createSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID memberId
    ) {
        CreateSessionResponse data = sessionService.createSession(sessionId, memberId);
        return ApiResponse.created(data, "세션이 생성되었습니다.");
    }

    @PostMapping("/{sessionId}/questions/{questionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateSessionResponse> createQuestion(
            @PathVariable UUID sessionId,
            @PathVariable UUID questionId,
            @RequestHeader("X-User-Id") UUID memberId
    ) {
        CreateSessionResponse data = sessionService.createQuestion(sessionId, questionId, memberId);
        return ApiResponse.created(data, "세션이 생성되었습니다.");
    }
}
