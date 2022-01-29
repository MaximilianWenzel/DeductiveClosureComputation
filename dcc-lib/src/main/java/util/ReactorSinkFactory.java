package util;

import reactor.core.publisher.Sinks;

public class ReactorSinkFactory {
    
    public static <T> Sinks.Many<T> getSink() {
        return Sinks.many().unicast().onBackpressureBuffer();
    }
    public static <T> Sinks.Many<T> getSocketManagerSink() {
        return Sinks.many().unicast().onBackpressureBuffer();
    }
}
