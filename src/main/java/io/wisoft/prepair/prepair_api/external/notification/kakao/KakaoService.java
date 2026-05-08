package io.wisoft.prepair.prepair_api.external.notification.kakao;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private final RestClient restClient;
    private static final String URL = "https://kapi.kakao.com/v2/api/talk/memo/default/send";

    @Retryable(
            value = {HttpServerErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void send(String kakaoAccessToken, String question, String questionTag) {
        String templateJson = """
                {
                  "object_type": "text",
                  "text": "[오늘의 면접 질문] 🎯\\n🏷 태그: %s\\n\\n💬 %s\\n\\n👉 prepair.wisoft.dev",
                  "link": {
                    "web_url": "https://prepair.wisoft.dev/"
                  }
                }
                """.formatted(questionTag, question);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("template_object", templateJson);

        restClient.post()
                .uri(URL)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
