package io.wisoft.prepair.prepair_api.external.member.dto;

import io.wisoft.prepair.prepair_api.external.member.enums.Frequency;
import io.wisoft.prepair.prepair_api.external.member.enums.Notification;

import java.util.List;
import java.util.UUID;

public record MemberInfo(
        UUID id,
        String email,
        String job,
        String nickname,
        Notification notification,
        Frequency frequency,
        Integer point,
        Boolean isPro,
        List<String> activity
) {
}
