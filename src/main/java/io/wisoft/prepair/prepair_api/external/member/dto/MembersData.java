package io.wisoft.prepair.prepair_api.external.member.dto;

import java.util.List;

public record MembersData(
        List<MemberSchedulerInfo> members,
        int total,
        int limit,
        int offset
) {
}