package networking.messages;

import data.Closure;

import java.io.Serializable;

public interface MessageModelVisitor<C extends Closure<A>, A extends Serializable, T extends Serializable> {
    void visit(InitializeWorkerMessage<C, A, T> message);

    void visit(StateInfoMessage message);

    void visit(SaturationAxiomsMessage<C, A, T> message);

    void visit(DebugMessage message);

    void visit(AcknowledgementMessage message);
}
