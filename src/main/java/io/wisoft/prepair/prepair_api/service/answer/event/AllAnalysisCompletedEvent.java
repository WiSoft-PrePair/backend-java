package io.wisoft.prepair.prepair_api.service.answer.event;

import java.nio.file.Path;
import java.util.UUID;

public record AllAnalysisCompletedEvent(UUID answerId, boolean hasFailed, Path videoPath) {
}