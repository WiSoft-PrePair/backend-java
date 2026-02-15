package io.wisoft.prepair.prepair_api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record JobPostingRequest(
        @NotBlank(message = "URL은 필수입니다.")
        @URL(message = "올바른 URL 형식이 아닙니다.")
        String url
) {
}