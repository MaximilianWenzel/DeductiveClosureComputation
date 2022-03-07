package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * subClassOf(x, x) :- type(x, rdfs:Class) .
 */
public class RuleRDFS10 extends Rule<RDFSClosure, TripleID> {

    long rdfTypeID;
    long rdfsClassID;
    long subClassOfID;

    @Override
    public Stream<TripleID> streamOfConclusions(TripleID axiom) {
        if (axiom.getPredicate() != rdfTypeID || axiom.getObject() != rdfsClassID) {
            return Stream.empty();
        }
        long x = axiom.getSubject();
        return Stream.of(new TripleID(x, subClassOfID, x));
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.rdfsClassID = closure.getRDFSClassID();
        this.rdfTypeID = closure.getRDFTypeID();
        this.subClassOfID = closure.getSubClassOfID();
    }
}
