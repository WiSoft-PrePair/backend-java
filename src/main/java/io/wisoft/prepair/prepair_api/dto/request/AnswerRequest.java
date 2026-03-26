package io.wisoft.prepair.prepair_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnswerRequest(
        @NotBlank String answer
) {
}
