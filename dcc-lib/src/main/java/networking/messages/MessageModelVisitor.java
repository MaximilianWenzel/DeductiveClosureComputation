package networking.messages;

import data.Closure;

import java.io.Serializable;

/**
 * A visitor which can be used in order to process a given message model object.
 * @param <C> Type of the resulting deductive closure of the saturation.
 * @param <A> Type of the axioms in the deductive closure of the saturation.
 */
public interface MessageModelVisitor<C extends Closure<A>, A extends Serializable> {
    void visit(InitializeWorkerMessage<C, A> message);

    void visit(StateInfoMessage message);

    void visit(DebugMessage message);

    void visit(AcknowledgementMessage message);

    void visit(StatisticsMessage message);

    void visit(AxiomCount message);

    void visit(RequestAxiomMessageCount message);
}
