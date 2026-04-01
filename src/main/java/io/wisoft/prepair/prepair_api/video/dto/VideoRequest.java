package io.wisoft.prepair.prepair_api.video.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record VideoRequest(
        String model,
        List<VideoMessage> messages,
        int max_tokens,
        Map<String, String> response_format
) {
    public static VideoRequest of(String model, String prompt, List<String> base64Images) {
        List<VideoMessage.Content> contents = new ArrayList<>();
        contents.add(new VideoMessage.TextContent(prompt));

        for (String base64 : base64Images) {
            contents.add(VideoMessage.ImageContent.ofBase64(base64));
        }

        VideoMessage message = new VideoMessage("user", contents);
        return new VideoRequest(model, List.of(message), 2000, Map.of("type", "json_object"));
    }
}