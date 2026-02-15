package io.wisoft.prepair.prepair_api.client.user.dto;


import java.util.List;
import java.util.UUID;

public record UserInfo(
        UUID id,
        String email,
        String job,
        String nickname,
        String notification,
        String frequency,
        Integer point,
        Boolean isPro,
        List<String> activity  // activity 필드 추가
) {
}
