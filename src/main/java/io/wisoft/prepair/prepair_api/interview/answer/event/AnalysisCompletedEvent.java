package io.wisoft.prepair.prepair_api.interview.answer.event;

import java.util.UUID;

public record AllAnalysisCompletedEvent(UUID answerId, boolean hasFailed) {
}
