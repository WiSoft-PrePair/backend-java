package io.wisoft.prepair.prepair_api.client.user.dto;

public record UserServiceResponse(
        int statusCode,
        UserInfo data,
        String message
) {
}
