package io.wisoft.prepair.prepair_api.global.common;

public record ApiResponse<T>(
        int statusCode,
        T data,
        String message
) {
}
