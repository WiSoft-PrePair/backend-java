package io.wisoft.prepair.prepair_api.interview.answer.event;

import io.wisoft.prepair.prepair_api.external.storage.LocalFileStorage;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisCompletionTracker {

    private final ApplicationEventPublisher eventPublisher;
    private final LocalFileStorage localFileStorage;

    private static final int ANALYSIS_TASKS = 2;
    private static final int TOTAL_TASKS = 3;

    private final ConcurrentMap<UUID, TrackingState> stateMap = new ConcurrentHashMap<>();

    public void init(UUID answerId, Path videoPath) {
        stateMap.put(answerId, new TrackingState(videoPath));
    }

    public void completeAnalysis(UUID answerId) {
        finishAnalysis(answerId, false);
    }

    public void failAnalysis(UUID answerId) {
        finishAnalysis(answerId, true);
    }

    public void completeS3(UUID answerId) {
        finishTask(answerId);
    }

    public void failS3(UUID answerId) {
        finishTask(answerId);
    }

    private void finishAnalysis(UUID answerId, boolean failed) {
        TrackingState state = stateMap.get(answerId);
        if (state == null) return;

        if (failed) state.analysisFailed.set(true);

        int analysisCount = state.analysisCount.incrementAndGet();
        if (analysisCount == ANALYSIS_TASKS) {
            eventPublisher.publishEvent(
                    new AnalysisCompletedEvent(answerId, state.analysisFailed.get())
            );
        }
        finishTask(answerId);
    }

    private void finishTask(UUID answerId) {
        TrackingState state = stateMap.get(answerId);
        if (state == null) return;

        int totalCount = state.totalCount.incrementAndGet();
        if (totalCount == TOTAL_TASKS) {
            stateMap.remove(answerId);
            localFileStorage.delete(state.videoPath);
        }
    }

    private static final class TrackingState {
        final AtomicInteger analysisCount = new AtomicInteger(0);
        final AtomicInteger totalCount = new AtomicInteger(0);
        final AtomicBoolean analysisFailed = new AtomicBoolean(false);
        final Path videoPath;

        TrackingState(Path videoPath) {
            this.videoPath = videoPath;
        }
    }
}
