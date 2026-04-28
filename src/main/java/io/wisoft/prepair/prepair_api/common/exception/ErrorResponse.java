package io.wisoft.prepair.prepair_api.common.exception;

public record ErrorResponse(
        int statusCode,
        ErrorDetail error
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getHttpStatus().value(),
                new ErrorDetail(errorCode.name(), errorCode.getMessage()));
    }
}
