package io.wisoft.prepair.prepair_api.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        try {
            setMdcContext(e, e.getMessage());
            log.warn("BusinessException occurred", e);
            return ResponseEntity
                    .status(e.getErrorCode().getHttpStatus())
                    .body(ErrorResponse.of(e.getErrorCode()));
        } finally {
            clearMdcContext();
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        try {
            setMdcContext(e, ErrorCode.INVALID_INPUT.getMessage());
            log.warn("Validation error occurred", e);
            return ResponseEntity
                    .status(ErrorCode.INVALID_INPUT.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
        } finally {
            clearMdcContext();
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        try {
            setMdcContext(e, ErrorCode.INVALID_PATH_VARIABLE.getMessage());
            log.warn("MethodArgumentTypeMismatchException occurred", e);
            return ResponseEntity
                    .status(ErrorCode.INVALID_PATH_VARIABLE.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.INVALID_PATH_VARIABLE));
        } finally {
            clearMdcContext();
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        try {
            setMdcContext(e, ErrorCode.INVALID_JSON_FORMAT.getMessage());
            log.warn("HttpMessageNotReadableException occurred", e);
            return ResponseEntity
                    .status(ErrorCode.INVALID_JSON_FORMAT.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.INVALID_JSON_FORMAT));
        } finally {
            clearMdcContext();
        }
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        try {
            setMdcContext(e, ErrorCode.METHOD_NOT_ALLOWED.getMessage());
            log.warn("HttpRequestMethodNotSupportedException occurred", e);
            return ResponseEntity
                    .status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED));
        } finally {
            clearMdcContext();
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        try {
            setMdcContext(e, ErrorCode.VIDEO_FILE_TOO_LARGE.getMessage());
            log.warn("File size exceeded", e);
            return ResponseEntity
                    .status(ErrorCode.VIDEO_FILE_TOO_LARGE.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.VIDEO_FILE_TOO_LARGE));
        } finally {
            clearMdcContext();
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        try {
            setMdcContext(e, ErrorCode.INTERNAL_ERROR.getMessage());
            log.error("Unexpected error occurred", e);
            return ResponseEntity
                    .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
        } finally {
            clearMdcContext();
        }
    }

    private void setMdcContext(Exception e, String errorMessage) {
        MDC.put("errorType", e.getClass().getSimpleName());
        MDC.put("errorMessage", errorMessage);
    }

    private void clearMdcContext() {
        MDC.remove("errorType");
        MDC.remove("errorMessage");
    }
}
