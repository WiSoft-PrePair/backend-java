package io.wisoft.prepair.prepair_api.entity.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Frequency {
    @JsonProperty("every") EVERY,
    @JsonProperty("weekly") WEEKLY
}
