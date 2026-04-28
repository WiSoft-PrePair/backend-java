package io.wisoft.prepair.prepair_api.external.openai.dto;

import java.util.List;

public record OpenAiRequest(
        String model,
        List<Message> messages
) {

    public static OpenAiRequest of(String model, String prompt) {
        return new OpenAiRequest(
                model,
               List.of(new Message("user", prompt))
        );
    }
}
