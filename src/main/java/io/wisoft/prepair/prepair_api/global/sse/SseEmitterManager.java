package io.wisoft.prepair.prepair_api.global.sse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class SseEmitterManager {

    private static final long TIMEOUT = 5 * 60 * 1000L;
    private final ConcurrentMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter create(UUID id) {
        SseEmitter existing = emitters.remove(id);
        if (existing != null) {
            existing.complete();
        }

        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(id, emitter);

        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        emitter.onError(e -> emitters.remove(id));

        return emitter;
    }

    public void send(UUID id, String eventName, Object data) {
        SseEmitter emitter = emitters.get(id);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.error("[SSE] 전송 실패 - id: {}", id, e);
            complete(id);
        }
    }

    public void complete(UUID id) {
        SseEmitter emitter = emitters.remove(id);
        if (emitter != null) {
            emitter.complete();
        }
    }
}