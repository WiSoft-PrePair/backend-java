package io.wisoft.prepair.prepair_api.global.exception;

public record ErrorResponse(
        int statusCode,
        ErrorDetail error
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getHttpStatus().value(),
                new ErrorDetail(errorCode.name(), errorCode.getMessage()));
    }
}
