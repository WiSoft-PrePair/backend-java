package io.wisoft.prepair.prepair_api.external.openai.dto;

import java.util.List;

public record QuestionWithTags(
        String question,
        List<String> tags
) {
    public String joinTags() {
        return String.join(",", tags);
    }
}
