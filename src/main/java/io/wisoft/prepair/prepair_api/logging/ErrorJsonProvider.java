package io.wisoft.prepair.prepair_api.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractJsonProvider;

import java.io.IOException;

/**
 * 예외 발생 시 중첩 error 객체를 로그에 추가하는 커스텀 provider.
 * <pre>
 *   "error": {
 *       "type": "ValidationException",
 *       "message": "Invalid email format"
 *   }
 * </pre>
 */
public class ErrorJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable == null) {
            return;
        }

        String className = throwable.getClassName();
        String simpleName = className.contains(".")
                ? className.substring(className.lastIndexOf('.') + 1)
                : className;

        generator.writeObjectFieldStart("error");
        generator.writeStringField("type", simpleName);
        generator.writeStringField("message", throwable.getMessage());
        generator.writeEndObject();
    }
}