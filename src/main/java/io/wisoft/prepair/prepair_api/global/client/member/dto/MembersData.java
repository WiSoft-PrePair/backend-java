package io.wisoft.prepair.prepair_api.global.client.member.dto;

import java.util.List;

public record MembersData(
        List<MemberInfo> members,
        int total,
        int limit,
        int offset
) {
}