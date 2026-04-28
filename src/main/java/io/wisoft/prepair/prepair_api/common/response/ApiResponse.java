package io.wisoft.prepair.prepair_api.common.response;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(
        int statusCode,
        T data,
        String message
) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(HttpStatus.OK.value(), data, message);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(HttpStatus.CREATED.value(), data, message);
    }

    public static <T> ApiResponse<T> accepted(T data, String message) {
        return new ApiResponse<>(HttpStatus.ACCEPTED.value(), data, message);
    }
}
