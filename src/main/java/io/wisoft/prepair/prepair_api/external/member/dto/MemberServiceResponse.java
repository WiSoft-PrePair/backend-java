package io.wisoft.prepair.prepair_api.external.member.dto;

public record MemberServiceResponse<T>(
        int statusCode,
        T data,
        String message
) {
}
