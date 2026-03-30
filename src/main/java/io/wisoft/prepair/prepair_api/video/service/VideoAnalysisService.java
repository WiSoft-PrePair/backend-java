package io.wisoft.prepair.prepair_api.video.service;

import io.wisoft.prepair.prepair_api.dto.FeedbackResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoAnalysisService {
    public FeedbackResult analyze(final MultipartFile video, final String question) {
    }
}
