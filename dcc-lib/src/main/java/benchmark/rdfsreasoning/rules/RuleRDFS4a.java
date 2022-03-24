package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * type(x, rdfs:Resource) :- rel(x, a, y) .
 */
public class RuleRDFS4a extends Rule<RDFSClosure, TripleID> {
    long rdfTypeID;

    @Override
    public Stream<TripleID> streamOfConclusions(TripleID axiom) {
        // given: rel(x, a, y)
        long x = axiom.getSubject();
        return Stream.of(new TripleID(x, rdfTypeID, closure.getRDFSResourceID()));
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.rdfTypeID = closure.getRDFTypeID();
    }
}
