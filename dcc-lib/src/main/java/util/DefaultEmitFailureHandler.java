package util;

import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;

public class DefaultEmitFailureHandler implements Sinks.EmitFailureHandler {
    @Override
    public boolean onEmitFailure(SignalType signalType, Sinks.EmitResult emitResult) {
        System.err.println(signalType);
        System.err.println(emitResult);
        return false;
    }
}
