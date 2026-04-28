package io.wisoft.prepair.prepair_api.interview.answer.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerRequest(
        @NotBlank String answer
) {
}
