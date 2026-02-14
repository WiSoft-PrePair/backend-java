package io.wisoft.prepair.prepair_api.global.client.member.dto;

import java.util.List;
import java.util.UUID;

public record MemberInfo(
        UUID id,
        String email,
        String job,
        String nickname,
        String notification,
        String frequency,
        Integer point,
        Boolean isPro,
        List<String> activity
) {
}
