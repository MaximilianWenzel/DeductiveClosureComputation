package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * type(y, rdfs:Resource) :- rel(x, a, y) .
 */
public class RuleRDFS4b extends Rule<RDFSClosure, TripleID> {
    long rdfTypeID;

    @Override
    public Stream<TripleID> streamOfInferences(TripleID axiom) {
        // given: rel(x, a, y)
        long y = axiom.getObject();

        if (closure.isLiteral(y)) {
            return Stream.empty();
        }

        return Stream.of(new TripleID(y, rdfTypeID, closure.getRDFSResourceID()));
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.rdfTypeID = closure.getRDFTypeID();
    }
}
