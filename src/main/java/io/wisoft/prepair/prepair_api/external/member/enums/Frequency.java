package io.wisoft.prepair.prepair_api.external.member.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Frequency {
    @JsonProperty("every") EVERY,
    @JsonProperty("weekly") WEEKLY
}
