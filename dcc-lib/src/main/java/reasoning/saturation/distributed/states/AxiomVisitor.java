package reasoning.saturation.distributed.states;

public interface AxiomVisitor<A> {

    void visit(A axiom);
}
