package io.wisoft.prepair.prepair_api.controller.dto.request;

import io.wisoft.prepair.prepair_api.entity.enums.AnswerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerRequest(
        @NotBlank String answer,
        @NotNull AnswerType answerType,
        String mediaUrl
) {
}
