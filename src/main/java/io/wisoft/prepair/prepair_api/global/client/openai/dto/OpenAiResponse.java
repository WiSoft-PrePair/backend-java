package io.wisoft.prepair.prepair_api.global.client.openai.dto;

import java.util.List;

public record OpenAiResponse(
        List<Choice> choices
) {
    public record Choice(
            Message message
    ) {
    }

    public String getContent() {
        return choices.get(0).message().content();
    }
}
