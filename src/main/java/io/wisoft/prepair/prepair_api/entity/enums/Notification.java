package io.wisoft.prepair.prepair_api.entity.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Notification {
    @JsonProperty("email") EMAIL,
    @JsonProperty("kakao") KAKAO,
    @JsonProperty("both") BOTH
}
