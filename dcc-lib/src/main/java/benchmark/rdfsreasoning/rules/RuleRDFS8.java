package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * subClassOf(x, rdfs:Resource) :- type(x, rdfs:Class) .
 */
public class RuleRDFS8 extends Rule<RDFSClosure, TripleID> {
    long rdfsClassID;
    long rdfTypeID;
    long subClassOfID;
    long rdfsResourceID;

    @Override
    public Stream<TripleID> streamOfConclusions(TripleID axiom) {
        if (axiom.getObject() != rdfsClassID || axiom.getPredicate() != rdfTypeID) {
            return Stream.empty();
        }
        long x = axiom.getSubject();
        return Stream.of(new TripleID(x, subClassOfID, rdfsResourceID));
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.rdfsClassID = closure.getRDFSClassID();
        this.rdfTypeID = closure.getRDFTypeID();
        this.subClassOfID = closure.getSubClassOfID();
        this.rdfsResourceID = closure.getRDFSResourceID();
    }
}
