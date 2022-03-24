package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * subPropertyOf(x, x) :- type(x, rdf:Property) .
 */
public class RuleRDFS6 extends Rule<RDFSClosure, TripleID> {
    long subPropertyOfID;
    long rdfTypeID;

    @Override
    public Stream<TripleID> streamOfConclusions(TripleID axiom) {
        // given: type(x, rdf:Property)
        if (axiom.getPredicate() != rdfTypeID) {
            return Stream.empty();
        }
        long x = axiom.getSubject();
        return Stream.of(new TripleID(x, subPropertyOfID, x));
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.subPropertyOfID = closure.getSubPropertyOfID();
        this.rdfTypeID = closure.getRDFTypeID();
    }
}
