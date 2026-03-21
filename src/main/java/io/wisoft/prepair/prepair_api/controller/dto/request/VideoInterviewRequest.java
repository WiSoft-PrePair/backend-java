package io.wisoft.prepair.prepair_api.controller.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VideoInterviewRequest(
        @NotNull
        @Min(value = 1, message = "면접 질문을 1개 이상부터 가능합니다.")
        Integer count
) {

}
