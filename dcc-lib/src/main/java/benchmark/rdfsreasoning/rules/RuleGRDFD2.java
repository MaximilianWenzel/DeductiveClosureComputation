package benchmark.rdfsreasoning.rules;

import benchmark.rdfsreasoning.RDFSClosure;
import org.rdfhdt.hdt.triples.TripleID;
import reasoning.rules.Rule;

import java.util.stream.Stream;

/**
 * type(a, rdf:Property) :- rel(x, a, y) .
 */
public class RuleGRDFD2 extends Rule<RDFSClosure, TripleID> {

    long rdfPropertyID;
    long rdfTypeID;


    @Override
    public Stream<TripleID> streamOfConclusions(TripleID axiom) {
        return Stream.of(new TripleID(axiom.getPredicate(), rdfTypeID, rdfPropertyID));
    }

    public void setClosure(RDFSClosure closure) {
        this.closure = closure;
        this.rdfPropertyID = closure.getRDFPropertyID();
        this.rdfTypeID = closure.getRDFTypeID();
    }
}
