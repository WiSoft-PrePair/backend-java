package io.wisoft.prepair.prepair_api.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    INVALID_PATH_VARIABLE(HttpStatus.BAD_REQUEST, "잘못된 경로 변수입니다."),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "올바르지 않은 JSON 형식입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // Resource
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),

    // Question
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),

    // Crawling
    CRAWLING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "채용공고 크롤링에 실패했습니다."),

    // Email
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),

    // Member Service
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),
    MEMBER_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Member 서비스에서 오류가 발생했습니다."),
    MEMBER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Member 서비스에 연결할 수 없습니다."),
    KAKAO_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "카카오 재로그인이 필요합니다."),

    // OpenAI
    OPENAI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI API 호출 중 오류가 발생했습니다."),
    OPENAI_RESPONSE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI 응답 파싱에 실패했습니다."),
    OPENAI_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI 응답이 유효하지 않습니다."),
    STT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "음성 텍스트 변환에 실패했습니다."),

    // Storage
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    VIDEO_CONVERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "영상 변환에 실패했습니다."),
    VIDEO_FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 제한을 초과했습니다. (최대 150MB)");


    private final HttpStatus httpStatus;
    private final String message;
}
