package io.wisoft.prepair.prepair_api.global.client.member.dto;

public record MemberServiceResponse<T>(
        int statusCode,
        T data,
        String message
) {
}
