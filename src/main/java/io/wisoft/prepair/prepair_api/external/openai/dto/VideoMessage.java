package io.wisoft.prepair.prepair_api.external.openai.dto;

import java.util.List;

public record VideoMessage(
        String role,
        List<Content> content
) {
    public sealed interface Content permits TextContent, ImageContent {}

    public record TextContent(String type, String text) implements Content {
        public TextContent(String text) {
            this("text", text);
        }
    }

    public record ImageContent(String type, ImageUrl image_url) implements Content {
        public static ImageContent ofBase64(String base64) {
            return new ImageContent("image_url", new ImageUrl("data:image/jpeg;base64," + base64, "low"));
        }
    }

    public record ImageUrl(String url, String detail) {}
}