package io.wisoft.prepair.prepair_api.service;

import io.wisoft.prepair.prepair_api.entity.InterviewQuestion;
import io.wisoft.prepair.prepair_api.global.client.openai.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
@RequiredArgsConstructor
public class SpeechToTextService {

    private final OpenAiClient openAiClient;

    public String transcribe(MultipartFile video, String tags) {
        return openAiClient.transcribe(video, tags);
    }
}
