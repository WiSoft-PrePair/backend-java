package io.wisoft.prepair.prepair_api.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // Resource
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),

    // Authorization
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Crawling
    CRAWLING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "채용공고 크롤링에 실패했습니다."),

    // Email Service
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),

    // Member Service
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),
    MEMBER_JOB_NOT_FOUND(HttpStatus.BAD_REQUEST, "멤버의 직무 정보가 없습니다."),
    MEMBER_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Member 서비스에서 오류가 발생했습니다."),
    MEMBER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Member 서비스에 연결할 수 없습니다."),

    // OpenAI
    OPENAI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI API 호출 중 오류가 발생했습니다."),
    OPENAI_RESPONSE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI 응답 파싱에 실패했습니다."),
    OPENAI_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI 응답이 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
