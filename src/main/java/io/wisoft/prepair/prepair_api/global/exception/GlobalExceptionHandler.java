package io.wisoft.prepair.prepair_api.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        try {
            setMdcContext(e, e.getMessage());
            log.error("BusinessException occurred", e);

            return ResponseEntity
                    .status(errorCode.getHttpStatus())
                    .body(ErrorResponse.of(errorCode));

        } finally {
            clearMdcContext();
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {

        try {
            setMdcContext(e, "잘못된 입력입니다.");
            log.error("Validation error occurred", e);

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));

        } finally {
            clearMdcContext();
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        try {
            setMdcContext(e, "서버 오류가 발생했습니다.");
            log.error("Unexpected error occurred", e);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));

        } finally {
            clearMdcContext();
        }
    }

    private void setMdcContext(Exception e, String errorMessage) {
        MDC.put("module", this.getClass().getSimpleName());
        MDC.put("errorType", e.getClass().getSimpleName());
        MDC.put("errorMessage", errorMessage);
    }

    private void clearMdcContext() {
        MDC.remove("module");
        MDC.remove("errorType");
        MDC.remove("errorMessage");
    }
}
