package io.wisoft.prepair.prepair_api.interview.answer.event;

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
    private static final int TOTAL_TASKS = 3;

    private final ConcurrentMap<UUID, AtomicInteger> completionMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, AtomicBoolean> failureMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Path> videoPathMap = new ConcurrentHashMap<>();

    public void init(UUID answerId, Path videoPath) {
        completionMap.put(answerId, new AtomicInteger(0));
        failureMap.put(answerId, new AtomicBoolean(false));
        videoPathMap.put(answerId, videoPath);
    }

    public void complete(UUID answerId) {
        finish(answerId, false);
    }

    public void fail(UUID answerId) {
        finish(answerId, true);
    }

    private void finish(UUID answerId, boolean failed) {
        AtomicInteger counter = completionMap.get(answerId);
        if (counter == null) return;

        if (failed) {
            failureMap.get(answerId).set(true);
        }

        int count = counter.incrementAndGet();

        if (count == TOTAL_TASKS) {
            boolean hasFailed = failureMap.remove(answerId).get();
            completionMap.remove(answerId);
            Path videoPath = videoPathMap.remove(answerId);
            eventPublisher.publishEvent(new AllAnalysisCompletedEvent(answerId, hasFailed, videoPath));
        }
    }
}