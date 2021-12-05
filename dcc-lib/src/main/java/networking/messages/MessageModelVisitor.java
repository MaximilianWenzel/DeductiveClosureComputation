package networking.messages;

import data.Closure;

import java.io.Serializable;

public interface MessageModelVisitor<C extends Closure<A>, A extends Serializable> {
    void visit(InitializeWorkerMessage<C, A> message);

    void visit(StateInfoMessage message);

    void visit(SaturationAxiomsMessage<C, A> message);

    void visit(DebugMessage message);

    void visit(AcknowledgementMessage message);
}
